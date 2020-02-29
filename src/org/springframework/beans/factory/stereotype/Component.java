package org.springframework.beans.factory.stereotype;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)//change policy. annotation is loading now to memory
public @interface Component {
}
