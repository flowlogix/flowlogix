/*
 * Copyright 2014 lprimak.
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
package com.flowlogix.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;

/**
 * Converts from any String to any type dynamically
 * 
 * @author lprimak
 */
@Slf4j
@SuppressWarnings("unchecked")
public class TypeConverter 
{
    public static<TT> TT valueOf(String strValue, Class<TT> type) throws IllegalArgumentException
    {
        boolean isInteger = false;
        if(type.equals(String.class))
        {
            return (TT)strValue;
        }        
        else if(type.equals(Double.class) || type.equals(double.class) 
                || type.equals(float.class) || type.equals(Float.class))
        {
            switch (strValue)
            {
                case "nan":
                    strValue = "NaN";
                    break;
                case "inf":
                    strValue = "Infinity";
                    break;
                case "-inf":
                    strValue = "-Infinity";
                    break;
                case "":
                    strValue = "0.0";
                    break;
            }
        }
        else if(type.equals(Integer.class) || type.equals(Long.class)
                || type.equals(int.class) || type.equals(long.class))
        {
            isInteger = true;
        }

        try
        {
            if(strValue == null)
            {
                return null;
            }
            else
            {
                return (TT)callValueOf(strValue, type);
            }
        }
        catch (Exception e)
        {
            if(isInteger)
            {
                try
                {
                    return (TT)(Long)((Double)callValueOf(strValue, Double.class)).longValue();
                }
                catch(Exception e1)
                {
                    throw new IllegalArgumentException(type.toString() + ": " + strValue, e);
                }
            }
            else
            {
                throw new IllegalArgumentException(type.toString() + ": " + strValue, e);
            }
        }
    }

    
    /**
     * Convert string to object given a type name
     * 
     * @param strValue
     * @param type
     * @return object after conversion
     * @throws IllegalArgumentException 
     */
    public static Object valueOf(String strValue, String type) throws IllegalArgumentException
    {
        String dataTypeString = type;
        if(type.equals(String.class.getName()))
        {
            // special case for strings
            dataTypeString = null;
        }
        Object value = strValue;
        if (dataTypeString != null)
        {
            try
            {
                Class<?> dataObjectClass = Class.forName(dataTypeString);
                value = valueOf(strValue, dataObjectClass);
            } 
            catch (ClassNotFoundException e)
            {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.append(e.toString() + " Value: " + strValue + ", Type: " + type + ", Stack Trace: ");
                e.printStackTrace(pw);
                pw.flush();
                log.warn(pw.toString());
            }
        }

        return value;
    }
    
    
    /**
     * Check if conversion will succeed
     * 
     * @param value
     * @param type
     * @return true if conversion is good
     */
    public static boolean checkType(String value, Class<?> type)
    {
        try
        {
            Object cv = TypeConverter.valueOf(value, type);
            if (value.equals(cv.toString()))
            {
                return true;
            }
        } catch (IllegalArgumentException e)
        {
        }
        return false;
    }

    /**
     * @param strValue
     * @param type
     * @return
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private static Object callValueOf(String strValue, Class<?> type) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        Method valueOfMethod = type.getMethod(
                "valueOf", new Class<?>[]
                                     { String.class });
        return valueOfMethod.invoke(null, new Object[] { strValue });
    }
}
