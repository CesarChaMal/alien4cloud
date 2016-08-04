package org.alien4cloud.tosca.editor.operations.substitution;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Add a substitution type for a topology.
 */
@Getter
@Setter
public class AddCapabilitySubstitutionTypeOperation extends AbstractTopologyTemplateOperation {

    @NotBlank
    private String nodeTemplateName;

    @NotBlank
    private String substitutionCapabilityId;

    @NotBlank
    private String capabilityId;


    @Override
    public String commitMessage() {
        return "add capability type substitution for <" + capabilityId + "> of node <" + nodeTemplateName + "> for the topology <" + getTopologyId() + ">";
    }
}
