package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.ide.fus.collectors.IntelliJDeodorantCounterCollector;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.RefactoringType;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass.ExtractClassRefactoringType;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;

import java.util.Collections;

/**
 * Panel for God Class refactoring.
 */
public class GodClassPanel extends AbstractRefactoringPanel {
    private static final String[] COLUMN_NAMES = new String[]{IntelliJDeodorantBundle.message("god.class.panel.source.class.name"),
            IntelliJDeodorantBundle.message("god.class.panel.extractable.name"),
            IntelliJDeodorantBundle.message("god.class.panel.entity.placement")};

    private static final int REFACTOR_DEPTH = 4;

    public GodClassPanel(@NotNull AnalysisScope scope) {
        super(scope,
                "god.class.identification.indicator",
                new ExtractClassRefactoringType(),
                new GodClassTreeTableModel(Collections.emptyList(), COLUMN_NAMES),
                REFACTOR_DEPTH);
    }

    @Override
    protected void logFound(Project project, Integer total) {
        IntelliJDeodorantCounterCollector.getInstance().refactoringFound(project, "extract.class", total);
    }

    @Override
    protected void doRefactor(RefactoringType.AbstractCandidateRefactoring candidateRefactoring) {
        ExtractClassRefactoringType.AbstractExtractClassRefactoring abstractRefactoring = (ExtractClassRefactoringType.AbstractExtractClassRefactoring) getAbstractRefactoringFromAbstractCandidateRefactoring(candidateRefactoring);

        Project project = scope.getProject();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            removeHighlighters(project);
            GodClassUserInputDialog dialog = new GodClassUserInputDialog(abstractRefactoring, this);
            dialog.show();
            if (dialog.isOK()) {
                showRefreshingProposal();
            }
        });
    }
}