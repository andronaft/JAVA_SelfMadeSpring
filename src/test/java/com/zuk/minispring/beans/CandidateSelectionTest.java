package com.zuk.minispring.beans;

import com.zuk.minispring.fixtures.byname.LaserPrinter;
import com.zuk.minispring.fixtures.byname.Office;
import com.zuk.minispring.fixtures.composite.AllChannels;
import com.zuk.minispring.fixtures.composite.Channel;
import com.zuk.minispring.fixtures.composite.EmailChannel;
import com.zuk.minispring.fixtures.composite.SmsChannel;
import com.zuk.minispring.fixtures.qualifier.AlertService;
import com.zuk.minispring.fixtures.qualifier.EmailSender;
import com.zuk.minispring.fixtures.qualifier.MessageSender;
import com.zuk.minispring.fixtures.qualifier.NotificationService;
import com.zuk.minispring.fixtures.qualifier.PushSender;
import com.zuk.minispring.fixtures.qualifier.SmsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** How one bean is chosen when several match: @Qualifier, @Primary, the field name, or all of them as a List. */
class CandidateSelectionTest {

    private static final String QUALIFIER = "com.zuk.minispring.fixtures.qualifier";

    private BeanFactory beanFactory;

    @BeforeEach
    void setUp() {
        beanFactory = new BeanFactory();
    }

    @Test
    void primaryBeanWinsWhenSeveralMatch() {
        beanFactory.scan(QUALIFIER);

        assertInstanceOf(EmailSender.class, beanFactory.getBean(NotificationService.class).getDefaultSender());
        assertInstanceOf(EmailSender.class, beanFactory.getBean(MessageSender.class));
    }

    @Test
    void qualifierMatchesQualifierOnTheBean() {
        beanFactory.scan(QUALIFIER);

        assertInstanceOf(SmsSender.class, beanFactory.getBean(NotificationService.class).getFastSender());
    }

    @Test
    void qualifierMatchesBeanName() {
        beanFactory.scan(QUALIFIER);

        assertInstanceOf(PushSender.class, beanFactory.getBean(NotificationService.class).getPushSender());
        assertInstanceOf(PushSender.class, beanFactory.getBean(AlertService.class).getSender());
    }

    @Test
    void listReceivesEveryBeanOfTheTypeInRegistrationOrder() {
        beanFactory.scan(QUALIFIER);

        List<MessageSender> senders = beanFactory.getBean(NotificationService.class).getAllSenders();
        assertEquals(List.of("email: hi", "push: hi", "sms: hi"), senders.stream().map(s -> s.send("hi")).toList());
        assertEquals(senders, beanFactory.getBean(AlertService.class).getAllSenders());
        assertThrows(UnsupportedOperationException.class, () -> senders.add(null));
    }

    @Test
    void fieldNamePicksTheBeanWhenNothingElseDoes() {
        beanFactory.scan("com.zuk.minispring.fixtures.byname");

        assertInstanceOf(LaserPrinter.class, beanFactory.getBean(Office.class).getPrinter());
    }

    @Test
    void compositeReceivesItsPeersButNotItself() {
        beanFactory.scan("com.zuk.minispring.fixtures.composite");

        AllChannels all = beanFactory.getBean(AllChannels.class);
        assertEquals(List.of(beanFactory.getBean(EmailChannel.class), beanFactory.getBean(SmsChannel.class)),
                all.getChannels());
        assertSame(all, beanFactory.getBean(Channel.class));
    }

    @Test
    void twoPrimaryBeansAreStillAmbiguous() {
        beanFactory.scan("com.zuk.minispring.fixtures.twoprimaries");

        BeanCreationException e = assertThrows(BeanCreationException.class, beanFactory::preInstantiateSingletons);
        assertInstanceOf(NoUniqueBeanDefinitionException.class, e.getCause());
        assertTrue(e.getMessage().contains("more than one of them is marked @Primary"), e.getMessage());
    }

    @Test
    void unknownQualifierIsReported() {
        beanFactory.scan("com.zuk.minispring.fixtures.badqualifier");

        BeanCreationException e = assertThrows(BeanCreationException.class, beanFactory::preInstantiateSingletons);
        assertTrue(e.getMessage().contains("with qualifier 'missing'"), e.getMessage());
    }

    @Test
    void ambiguityErrorSuggestsPrimaryOrQualifier() {
        beanFactory.scan("com.zuk.minispring.fixtures.ambiguous");

        BeanCreationException e = assertThrows(BeanCreationException.class, beanFactory::preInstantiateSingletons);
        assertTrue(e.getMessage().contains("mark one with @Primary, or choose one with @Qualifier"), e.getMessage());
    }
}
