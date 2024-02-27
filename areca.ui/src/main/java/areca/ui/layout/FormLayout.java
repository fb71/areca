/*
 * Copyright (C) 2023, the @authors. All rights reserved.
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
package areca.ui.layout;

import areca.common.base.Consumer.RConsumer;
import areca.common.base.Predicate.RPredicate;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;

/**
 *
 * @author Falko Bräutigam
 */
public class FormLayout
        extends LayoutManager {

    private static final Log LOG = LogFactory.getLog( FormLayout.class );

    @Override
    public void layout( UIComposite composite ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    /**
     *
     */
    protected static class Element {
        static final Element OUTER = new Element();

        protected UIComponent component;
        public Edge         top, bottom, left, right;

        public void top( RPredicate<Edge> sel, RConsumer<Edge> perform ) {
            if (top.value == -1 && sel.test( top )) {
                perform.accept( top );
            }
        }
    }

    protected class Edge {
        public int          value = -1;
        public Constraint   constraint;
    }

    protected class Constraint {
        public int          fixed;
        public int          margin;
        public int          percent;
        public Edge         attachedTo;
    }

//    public static final RPredicate<Edge> FIXED = edge -> edge.constraint.fixed >= 0;
//    public static final RPredicate<Edge> ATTACHED_TO_OUTER = edge -> edge.constraint.attachedTo == Element.OUTER;

    protected interface Resolver {
        void resolve( Element elm );
    }

    /**
     *
     */
    protected class OuterEdgesResolver implements Resolver {
        @Override
        public void resolve( Element elm ) {
//            elm.top( FIXED.and( ATTACHED_TO_OUTER ), edge -> {
//                edge.value = edge.constraint.margin;
//            });
//            // ...
        }
    }
}
