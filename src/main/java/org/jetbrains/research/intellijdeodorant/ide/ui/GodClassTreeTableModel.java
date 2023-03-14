package org.jetbrains.research.intellijdeodorant.ide.ui;

import org.jetbrains.research.intellijdeodorant.core.distance.ExtractClassCandidateGroup;
import org.jetbrains.research.intellijdeodorant.core.distance.ExtractClassCandidateRefactoring;
import org.jetbrains.research.intellijdeodorant.core.distance.ExtractedConcept;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.RefactoringType;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass.ExtractClassRefactoringType;

import java.util.ArrayList;
import java.util.List;

public class GodClassTreeTableModel extends AbstractTreeTableModel {
    public GodClassTreeTableModel(List<RefactoringType.AbstractCandidateRefactoringGroup> candidateRefactoringGroups, String[] columnNames) {
        super(candidateRefactoringGroups, columnNames, new ExtractClassRefactoringType());
    }

    @Override
    public Object getValueAt(Object o, int index) {
        if (o instanceof ExtractClassRefactoringType.AbstractExtractClassCandidateRefactoringGroup) {
            ExtractClassRefactoringType.AbstractExtractClassCandidateRefactoringGroup abstractExtractClassCandidateRefactoringGroup =
                    (ExtractClassRefactoringType.AbstractExtractClassCandidateRefactoringGroup) o;
            ExtractClassCandidateGroup group = (ExtractClassCandidateGroup) abstractExtractClassCandidateRefactoringGroup.getCandidateRefactoringGroup();

            if (index == 0) {
                return group.getSource();
            } else {
                return "";
            }
        } else if (o instanceof ExtractClassRefactoringType.AbstractExtractClassCandidateRefactoring) {
            ExtractClassRefactoringType.AbstractExtractClassCandidateRefactoring abstractCandidateRefactoring = (ExtractClassRefactoringType.AbstractExtractClassCandidateRefactoring) o;
            ExtractClassCandidateRefactoring candidateRefactoring = (ExtractClassCandidateRefactoring) abstractCandidateRefactoring.getCandidateRefactoring();
            switch (index) {
                case 0:
                    return "";
                case 1:
                    return candidateRefactoring.getExtractedEntities().size();
                case 2:
                    return candidateRefactoring.getWANOfEntities() + "/" + candidateRefactoring.getRefactoredWAN();
            }
        }

        return "";
    }

    @Override
    public List<?> getChildren(Object parent) {
        if (parent instanceof ExtractClassRefactoringType.AbstractExtractClassCandidateRefactoringGroup) {
            ExtractClassRefactoringType.AbstractExtractClassCandidateRefactoringGroup abstractGroup = (ExtractClassRefactoringType.AbstractExtractClassCandidateRefactoringGroup) parent;
            ExtractClassCandidateGroup group = (ExtractClassCandidateGroup) abstractGroup.getCandidateRefactoringGroup();

            return group.getExtractedConcepts();
        } else if (parent instanceof ExtractedConceptAndChildren) {
            ExtractedConceptAndChildren concept = (ExtractedConceptAndChildren) parent;
            return concept.children;
        } else {
            return super.getChildren(parent);
        }
    }

    @Override
    public Object getChild(Object parent, int index) {
        Object child = super.getChild(parent, index);

        if (parent instanceof ExtractClassRefactoringType.AbstractExtractClassCandidateRefactoringGroup
                && child instanceof ExtractedConcept) {
            return new ExtractedConceptAndChildren((ExtractedConcept) child);
        } else {
            return child;
        }
    }

    private static class ExtractedConceptAndChildren {
        private final ExtractedConcept extractedConcept;
        private final List<ExtractClassRefactoringType.AbstractExtractClassCandidateRefactoring> children;

        private ExtractedConceptAndChildren(ExtractedConcept extractedConcept) {
            this.extractedConcept = extractedConcept;
            ExtractClassCandidateRefactoring[] refactorings = extractedConcept.getConceptClusters().toArray(new ExtractClassCandidateRefactoring[0]);
            children = new ArrayList<>();
            for (ExtractClassCandidateRefactoring refactoring : refactorings) {
                children.add(new ExtractClassRefactoringType.AbstractExtractClassCandidateRefactoring(refactoring));
            }
        }

        @Override
        public String toString() {
            return extractedConcept.toString();
        }

        @Override
        public int hashCode() {
            return extractedConcept.hashCode();
        }
    }
}