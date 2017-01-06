package alien4cloud.tosca;

import java.util.List;

import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;

import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants;
import alien4cloud.utils.AlienConstants;
import alien4cloud.utils.AlienUtils;

/**
 * Utility to work with normative constants.
 */
public final class ToscaNormativeUtil {

    private ToscaNormativeUtil() {
    };

    /**
     * Convert a short-named normative interface name to a long one.
     * 
     * @param interfaceName The name of the interface.
     * @return If the interface name is a normative interface shortname then the fullname, if returns the interfaceName.
     */
    public static String getLongInterfaceName(String interfaceName) {
        if (ToscaNodeLifecycleConstants.STANDARD_SHORT.equalsIgnoreCase(interfaceName)) {
            return ToscaNodeLifecycleConstants.STANDARD;
        } else if (ToscaRelationshipLifecycleConstants.CONFIGURE_SHORT.equalsIgnoreCase(interfaceName)) {
            return ToscaRelationshipLifecycleConstants.CONFIGURE;
        }
        return interfaceName;
    }

    /**
     * Verify that the given {@link AbstractInheritableToscaType} is from the given type.
     *
     * @param indexedInheritableToscaElement The {@link AbstractInheritableToscaType} to verify.
     * @param type The type to match
     * @return <code>true</code> if the {@link AbstractInheritableToscaType} is from the given type.
     */
    public static boolean isFromType(String type, AbstractInheritableToscaType indexedInheritableToscaElement) {
        return isFromType(type, indexedInheritableToscaElement.getElementId(), indexedInheritableToscaElement.getDerivedFrom());
    }

    /**
     * Verify that the given <code>type</code> is or inherits the given <code>expectedType</code>.
     */
    public static boolean isFromType(String expectedType, String type, List<String> typeHierarchy) {
        return expectedType.equals(type) || (typeHierarchy != null && typeHierarchy.contains(expectedType));
    }

    public static String formatedOperationOutputName(String nodeName, String interfaceName, String operationName, String output) {
        return AlienUtils.prefixWith(AlienConstants.OPERATION_NAME_SEPARATOR, output, new String[] { nodeName, interfaceName, operationName });
    }
}
