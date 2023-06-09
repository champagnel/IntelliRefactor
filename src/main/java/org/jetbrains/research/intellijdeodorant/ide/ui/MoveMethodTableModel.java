package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;
import org.jetbrains.research.intellijdeodorant.core.distance.MoveMethodCandidateRefactoring;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.moveMethod.MoveMethodRefactoring;
import org.jetbrains.research.intellijdeodorant.utils.PsiUtils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class MoveMethodTableModel extends AbstractTableModel {
    static final int SELECTION_COLUMN_INDEX = 0;
    static final int ENTITY_COLUMN_INDEX = 1;
    static final int MOVE_TO_COLUMN_INDEX = 2;
    private static final int ACCESSED_MEMBERS_COUNT_INDEX = 3;
    private static final int COLUMNS_COUNT = 4;

    private final List<MoveMethodRefactoring> refactorings = new ArrayList<>();
    private final List<Integer> virtualRows = new ArrayList<>();
    private boolean[] isSelected;
    private List<MoveMethodCandidateRefactoring> candidateRefactorings = new ArrayList<>();

    MoveMethodTableModel(List<MoveMethodRefactoring> refactorings) {
        updateTable(refactorings);
    }

    void updateTable(List<MoveMethodRefactoring> refactorings) {
        this.refactorings.clear();
        this.refactorings.addAll(refactorings);
        isSelected = new boolean[refactorings.size()];
        IntStream.range(0, refactorings.size())
                .forEachOrdered(virtualRows::add);
        fireTableDataChanged();
    }

    void clearTable() {
        this.refactorings.clear();
        this.virtualRows.clear();
        isSelected = new boolean[0];
        fireTableDataChanged();
    }

    void selectAll() {
        for (int i = 0; i < virtualRows.size(); i++) {
            setValueAtRowIndex(true, i, false);
        }

        fireTableDataChanged();
    }

    void deselectAll() {
        for (int i = 0; i < virtualRows.size(); i++) {
            setValueAtRowIndex(false, i, false);
        }

        fireTableDataChanged();
    }

    void updateRows() {
        virtualRows.forEach(i -> {
            if (!refactorings.get(i).getOptionalMethod().isPresent()) {
                isSelected[i] = false;
            }
        });
        fireTableDataChanged();
    }

    List<MoveMethodRefactoring> pullSelected() {
        return virtualRows.stream()
                .filter(i -> isSelected[i] && refactorings.get(i).getOptionalMethod().isPresent())
                .map(refactorings::get)
                .collect(Collectors.toList());
    }

    @Override
    public int getColumnCount() {
        return COLUMNS_COUNT;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case SELECTION_COLUMN_INDEX:
                return "";
            case ENTITY_COLUMN_INDEX:
                return IntelliJDeodorantBundle.message("method.column.title");
            case MOVE_TO_COLUMN_INDEX:
                return IntelliJDeodorantBundle.message("move.to.column.title");
            case ACCESSED_MEMBERS_COUNT_INDEX:
                return IntelliJDeodorantBundle.message("dependencies.column.title");
        }
        throw new IndexOutOfBoundsException("Unexpected column index: " + column);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == SELECTION_COLUMN_INDEX && refactorings.get(rowIndex).getOptionalMethod().isPresent();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == SELECTION_COLUMN_INDEX ? Boolean.class : String.class;
    }

    @Override
    public int getRowCount() {
        return virtualRows.size();
    }

    @Override
    public void setValueAt(Object value, int virtualRow, int columnIndex) {
        final int rowIndex = virtualRows.get(virtualRow);
        final boolean isRowSelected = (Boolean) value;
        setValueAtRowIndex(isRowSelected, rowIndex, true);

        fireTableDataChanged();
    }

    private void setValueAtRowIndex(boolean isRowSelected, int rowIndex, boolean forceSelectInConflicts) {
        if (!refactorings.get(rowIndex).getOptionalMethod().isPresent()) {
            return;
        }

        boolean hasConflicts = updateConflictingRows(isRowSelected, rowIndex, forceSelectInConflicts);

        if (isRowSelected && hasConflicts && !forceSelectInConflicts) {
            return;
        }

        isSelected[rowIndex] = isRowSelected;
    }

    /**
     * Disables conflicting suggestions (i.e. the suggestions to move the same method) in the table.
     *
     * @param isRowSelected          true if the row is selected, false if the row is deselected.
     * @param rowIndex               index of row.
     * @param forceSelectInConflicts if true and there are any conflicts, the row shouldn't be marked as selected.
     * @return true if there are conflicting suggestions with the initial row, false otherwise.
     */
    private boolean updateConflictingRows(boolean isRowSelected, int rowIndex, boolean forceSelectInConflicts) {
        boolean hasConflicts = false;

        for (int i = 0; i < refactorings.size(); i++) {
            if (i == rowIndex) {
                continue;
            }

            if (refactorings.get(rowIndex).methodEquals(refactorings.get(i))) {
                hasConflicts = true;

                if (isRowSelected) {
                    if (!forceSelectInConflicts) {
                        return true;
                    }
                }
                isSelected[i] = false;
            }
        }

        return hasConflicts;
    }

    boolean isAnySelected() {
        for (boolean isSelectedItem : isSelected) {
            if (isSelectedItem) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Nullable
    public Object getValueAt(int virtualRow, int columnIndex) {
        final int rowIndex = virtualRows.get(virtualRow);
        switch (columnIndex) {
            case SELECTION_COLUMN_INDEX:
                return isSelected[rowIndex];
            case ENTITY_COLUMN_INDEX:
                Optional<PsiMethod> method = refactorings.get(rowIndex).getOptionalMethod();
                String qualifiedMethodName = refactorings.get(rowIndex).getQualifiedMethodName();
                return method.map(psiMethod -> qualifiedMethodName).orElseGet(() -> qualifiedMethodName + " | "
                        + IntelliJDeodorantBundle.message("java.member.is.not.valid"));
            case MOVE_TO_COLUMN_INDEX:
                Optional<PsiClass> targetClass = refactorings.get(rowIndex).getOptionalTargetClass();
                return targetClass.map(PsiUtils::getHumanReadableName).orElseGet(() -> IntelliJDeodorantBundle.message("target.class.is.not.valid"));
            case ACCESSED_MEMBERS_COUNT_INDEX:
                return candidateRefactorings.get(rowIndex).getOriginalEP()
                        + "/" + candidateRefactorings.get(rowIndex).getEntityPlacementOfSystem();
        }
        throw new IndexOutOfBoundsException("Unexpected column index: " + columnIndex);
    }

    Optional<? extends PsiMember> getUnitAt(int virtualRow, int column) {
        final int row = virtualRows.get(virtualRow);
        switch (column) {
            case ENTITY_COLUMN_INDEX:
                return refactorings.get(row).getOptionalMethod();
            case MOVE_TO_COLUMN_INDEX:
                return refactorings.get(row).getOptionalTargetClass();
        }
        throw new IndexOutOfBoundsException("Unexpected column index: " + column);
    }

    void setupRenderer(JTable table) {
        table.setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer() {
            private final JLabel EMPTY_LABEL = new JLabel();

            {
                EMPTY_LABEL.setBackground(JBColor.LIGHT_GRAY);
                EMPTY_LABEL.setOpaque(true);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSel, boolean hasFocus,
                                                           int row, int column) {
                final int realRow = virtualRows.get(table.convertRowIndexToModel(row));
                if (refactorings.get(realRow).getOptionalMethod().isPresent()) {
                    return super.getTableCellRendererComponent(table, value, isSel, hasFocus, row, column);
                } else {
                    return EMPTY_LABEL;
                }
            }
        });

        table.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int virtualRow, int column) {
                final int row = virtualRows.get(table.convertRowIndexToModel(virtualRow));
                if (!refactorings.get(row).getOptionalMethod().isPresent()) {
                    setBackground(JBColor.LIGHT_GRAY);
                } else if (isSelected) {
                    setBackground(table.getSelectionBackground());
                } else {
                    setBackground(table.getBackground());
                }
                setEnabled(refactorings.get(row).getOptionalMethod().isPresent());
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, virtualRow, column);
            }
        });
    }

    public List<MoveMethodCandidateRefactoring> getCandidateRefactorings() {
        return candidateRefactorings;
    }

    public void setCandidateRefactorings(List<MoveMethodCandidateRefactoring> candidateRefactorings) {
        this.candidateRefactorings = candidateRefactorings;
    }
}