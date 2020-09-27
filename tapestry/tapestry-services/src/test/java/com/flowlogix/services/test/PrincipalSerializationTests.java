/*
 * Copyright 2012 lprimak.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flowlogix.services.test;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.io.DefaultSerializer;
import org.apache.shiro.io.Serializer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author lprimak
 */
public class PrincipalSerializationTests
{
    @Test
    public void serializer()
    {
        final Serializer<X> ser = new DefaultSerializer<>();
        final String encoded = Base64.encodeToString(ser.serialize(new X(15, "xxx")));
        assertEquals(new X(15, "xxx"), ser.deserialize(Base64.decode(encoded)));
    }


    @EqualsAndHashCode
    @AllArgsConstructor
    static class X implements Serializable
    {
        final Integer x;;
        final String y;
    }
}
