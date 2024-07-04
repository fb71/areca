/*
 * Copyright (C) 2024, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package areca.ui.viewer.transform;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Date;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.DatePicker;
import areca.ui.component2.DatePicker.DateTime;
import areca.ui.viewer.model.Model;

/**
 * Transforms a {@link Date} into a String for {@link DatePicker}.
 *
 * @author Falko Bräutigam
 */
public class Date2StringTransform
        extends TransformingModelBase<Model<Date>, String>
        implements Model<String> {

    private static final Log LOG = LogFactory.getLog( Date2StringTransform.class );

    public static final SimpleDateFormat DATE = new SimpleDateFormat( "yyyy-MM-dd" );
    public static final SimpleDateFormat TIME = new SimpleDateFormat( "HH:mm" );
    public static final SimpleDateFormat DATETIME = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm" );

    protected SimpleDateFormat df;

    public Date2StringTransform( DateTime dateTime, Model<Date> delegate ) {
        super( delegate );
        switch (dateTime) {
            case DATE: df = DATE; break;
            case TIME: df = TIME; break;
            case DATETIME: df = DATETIME; break;
        }
    }


    @Override
    public ValidationResult validate( String value ) {
        try {
            if (isNotEmpty( value )) {
                df.parse( value );
            }
            return super.validate( value );
        }
        catch (ParseException e) {
            return new ValidationResult( e );
        }
    }


    @Override
    public String get() {
        var value = delegate.get();
        //LOG.warn( "get(): %s -> %s", value, df.format( value ) );
        return value != null ? df.format( value ) : null;
    }


    @Override
    public void set( String value ) {
        try {
            //LOG.warn( "set(): %s -> %s", value, df.parse( value ) );
            delegate.set( isNotEmpty( value ) ? df.parse( value ) : null );
        }
        catch (ParseException e) {
            throw new RuntimeException( e );
        }
    }

}
