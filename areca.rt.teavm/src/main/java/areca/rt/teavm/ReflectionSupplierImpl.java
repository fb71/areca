package areca.rt.teavm;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.teavm.classlib.ReflectionContext;
import org.teavm.classlib.ReflectionSupplier;
import org.teavm.model.ClassReader;
import org.teavm.model.FieldReader;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;

import areca.common.testrunner.Test;

/**
 *
 * @author Falko Bräutigam
 */
public class ReflectionSupplierImpl implements ReflectionSupplier {

    @Override
    public Collection<String> getAccessibleFields(ReflectionContext context, String className) {
        System.out.println( className + "... " + context );
        Set<String> result = new HashSet<>();
        ClassReader cls = context.getClassSource().get( className );
        if (cls != null) {
            for (FieldReader f : cls.getFields()) {
                f.getAnnotations().all().forEach( a -> System.out.println( f.getName() + " : " + a.getType() ) );
                if (f.getAnnotations().get( Test.class.getName() ) != null) {
                    System.out.println( "       Annotation: " + className + "@" + f.getName() + "()" );
                    result.add( f.getName() );
                }
            }
        }
        return result;
    }

    @Override
    public Collection<MethodDescriptor> getAccessibleMethods(ReflectionContext context, String className) {
        Set<MethodDescriptor> result = new HashSet<>();
        ClassReader cls = context.getClassSource().get( className );
        System.out.println( className + "... " + cls );
        if (cls != null && false) {
            for (MethodReader m : cls.getMethods()) {
                m.getAnnotations().all().forEach( a -> System.out.println( m.getName() + "() : " + a.getType() ) );
                if (m.getAnnotations().get( Test.class.getName() ) != null) {
                    System.out.println( "       Annotation: " + className + "@" + m.getName() + "()" );
                    result.add( m.getDescriptor() );
                }
            }
        }
        return result;
    }
}
