/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package areca.app.ui;

import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.apache.commons.lang3.time.DateUtils.addYears;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.commons.lang3.time.DateUtils;

import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 */
class DaySeparatorBorder {

    private static final Log LOG = LogFactory.getLog( DaySeparatorBorder.class );

    protected static final DateFormat df = SimpleDateFormat.getDateTimeInstance( DateFormat.MEDIUM, DateFormat.MEDIUM );

    public static final List<DaySeparatorBorder> borders = new ArrayList<>();

    static {
        var today = DateUtils.truncate( new Date(), Calendar.DATE );
        today = DateUtils.addHours( today, -12 ); // ???
        borders.add( new DaySeparatorBorder( "Today", today.getTime() ) );
        borders.add( new DaySeparatorBorder( "Yesterday", addDays( today, -1 ).getTime() ) );
        borders.add( new DaySeparatorBorder( "2 days ago", addDays( today, -2 ).getTime() ) );
        borders.add( new DaySeparatorBorder( "3 days ago", addDays( today, -3 ).getTime() ) );
        borders.add( new DaySeparatorBorder( "7 days ago", addDays( today, -7 ).getTime() ) );
        borders.add( new DaySeparatorBorder( "2 weeks ago", addDays( today, -14 ).getTime() ) );
        borders.add( new DaySeparatorBorder( "1 month ago", addDays( today, -30 ).getTime() ) );
        borders.add( new DaySeparatorBorder( "Older", addYears( today, -30 ).getTime() ) );
        LOG.debug( "Intervals: %s", Sequence.of( borders ).map( b -> df.format( b.start ) ) );
    }

    // instance *******************************************

    public String           label;

    public long             start; // time

    public DaySeparatorBorder( String label, long start ) {
        this.label = label;
        this.start = start;
    }
}