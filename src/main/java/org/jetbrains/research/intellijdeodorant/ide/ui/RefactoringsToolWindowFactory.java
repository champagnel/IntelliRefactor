package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;

class RefactoringsToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();
        AnalysisScope scope = new AnalysisScope(project);
        Content moveMethodPanel = contentManager.getFactory().createContent(new MoveMethodPanel(scope), IntelliJDeodorantBundle.message("feature.envy.dimension.name"), false);
        Content godClassPanel = contentManager.getFactory().createContent(new GodClassPanel(scope), IntelliJDeodorantBundle.message("god.class.dimension.name"), false);
        contentManager.addContent(moveMethodPanel);
        contentManager.addContent(godClassPanel);
    }

}
