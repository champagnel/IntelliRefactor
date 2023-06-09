package org.jetbrains.research.intellijdeodorant.core.distance;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import org.jetbrains.research.intellijdeodorant.core.GodClassVisualizationData;
import org.jetbrains.research.intellijdeodorant.core.ast.FieldObject;
import org.jetbrains.research.intellijdeodorant.core.ast.MethodObject;
import org.jetbrains.research.intellijdeodorant.core.ast.TypeObject;
import org.jetbrains.research.intellijdeodorant.utils.TopicFinder;

import java.util.*;

import static org.jetbrains.research.intellijdeodorant.ide.refactoring.Refactoring.DELIMITER;

public class ExtractClassCandidateRefactoring extends CandidateRefactoring implements Comparable<ExtractClassCandidateRefactoring> {
    private final MySystem system;
    private final MyClass sourceClass;
    private final List<Entity> extractedEntities;
    private final Map<MyMethod, Boolean> leaveDelegate;
    private final String targetClassName;
    private final GodClassVisualizationData visualizationData;
    private List<String> topics;
    private double WANOfEntities;
    private double refactoredWAN;

    public ExtractClassCandidateRefactoring(MySystem system, MyClass sourceClass, ArrayList<Entity> extractedEntities) {
        super();
        this.system = system;
        this.sourceClass = sourceClass;
        this.extractedEntities = extractedEntities;
        this.leaveDelegate = new LinkedHashMap<>();
        if (system.getClass(sourceClass.getName() + "Product") == null) {
            this.targetClassName = sourceClass.getClassObject().getPsiClass().getName() + "Product";
        } else {
            this.targetClassName = sourceClass.getClassObject().getPsiClass().getName() + "Product2";
        }
        this.topics = new ArrayList<>();
        Set<MethodObject> extractedMethods = new LinkedHashSet<>();
        Set<FieldObject> extractedFields = new LinkedHashSet<>();
        for (Entity entity : extractedEntities) {
            if (entity instanceof MyMethod) {
                MyMethod myMethod = (MyMethod) entity;
                extractedMethods.add(myMethod.getMethodObject());
            } else if (entity instanceof MyAttribute) {
                MyAttribute myAttribute = (MyAttribute) entity;
                extractedFields.add(myAttribute.getFieldObject());
            }
        }
        this.visualizationData = new GodClassVisualizationData(sourceClass.getClassObject(), extractedMethods, extractedFields);
    }

    public String getDefaultTargetClassName() {
        return targetClassName;
    }

    public List<Entity> getExtractedEntities() {
        return extractedEntities;
    }

    public Set<PsiMethod> getExtractedMethods() {
        Set<PsiMethod> extractedMethods = new LinkedHashSet<>();
        for (Entity entity : extractedEntities) {
            if (entity instanceof MyMethod) {
                MyMethod method = (MyMethod) entity;
                extractedMethods.add(method.getMethodObject().getMethodDeclaration());
            }
        }
        return extractedMethods;
    }

    public Set<PsiMethod> getDelegateMethods() {
        Set<PsiMethod> delegateMethods = new LinkedHashSet<>();
        for (MyMethod method : leaveDelegate.keySet()) {
            if (leaveDelegate.get(method))
                delegateMethods.add(method.getMethodObject().getMethodDeclaration());
        }
        return delegateMethods;
    }

    public Set<PsiField> getExtractedFieldFragments() {
        Map<Integer, PsiField> extractedFieldFragmentMap = new TreeMap<>();
        for (Entity entity : extractedEntities) {
            if (entity instanceof MyAttribute) {
                MyAttribute attribute = (MyAttribute) entity;
                int index = sourceClass.getAttributeList().indexOf(attribute);
                extractedFieldFragmentMap.put(index, attribute.getFieldObject().getVariableDeclaration());
            }
        }
        return new LinkedHashSet<>(extractedFieldFragmentMap.values());
    }

    public Map<MyMethod, Boolean> getLeaveDelegate() {
        return leaveDelegate;
    }

    public boolean isApplicable() {
        int methodCounter = 0;
        for (Entity entity : extractedEntities) {
            if (entity instanceof MyMethod) {
                MyMethod method = (MyMethod) entity;
                methodCounter++;
                if (isSynchronized(method) || containsSuperMethodInvocation(method) ||
                        overridesMethod(method) || method.isAbstract() || containsFieldAccessOfEnclosingClass(method) ||
                        isReadObject(method) || isWriteObject(method))
                    return false;
            } else if (entity instanceof MyAttribute) {
                MyAttribute attribute = (MyAttribute) entity;
                if (!attribute.getAccess().equals("private")) {
                    if (system.getSystemObject().containsFieldInstruction(attribute.getFieldObject().generateFieldInstruction(), sourceClass.getClassObject()))
                        return false;
                }
            }
        }
        return extractedEntities.size() > 2 && methodCounter != 0 && validRemainingMethodsInSourceClass()
                && validRemainingFieldsInSourceClass() && !visualizationData.containsNonAccessedFieldInExtractedClass();
    }

    private boolean validRemainingMethodsInSourceClass() {
        for (MyMethod sourceMethod : sourceClass.getMethodList()) {
            if (!extractedEntities.contains(sourceMethod)) {
                MethodObject methodObject = sourceMethod.getMethodObject();
                if (!methodObject.isStatic() && !methodObject.isAbstract() && methodObject.isGetter() == null
                        && methodObject.isSetter() == null && methodObject.isDelegate() == null &&
                        !isReadObject(methodObject) && !isWriteObject(methodObject) && !isEquals(methodObject)
                        && !isHashCode(methodObject) && !isClone(methodObject) && !isCompareTo(methodObject)
                        && !isToString(methodObject)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean validRemainingFieldsInSourceClass() {
        for (MyAttribute sourceAttribute : sourceClass.getAttributeList()) {
            if (!extractedEntities.contains(sourceAttribute)) {
                FieldObject fieldObject = sourceAttribute.getFieldObject();
                if (!fieldObject.isStatic()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isReadObject(MyMethod method) {
        return isReadObject(method.getMethodObject());
    }

    private boolean isReadObject(MethodObject methodObject) {
        List<TypeObject> parameterTypeList = methodObject.getParameterTypeList();
        return methodObject.getName().equals("readObject") && parameterTypeList.size() == 1
                && parameterTypeList.get(0).getClassType().equals("java.io.ObjectInputStream");
    }

    private boolean isWriteObject(MyMethod method) {
        return isWriteObject(method.getMethodObject());
    }

    private boolean isWriteObject(MethodObject methodObject) {
        List<TypeObject> parameterTypeList = methodObject.getParameterTypeList();
        return methodObject.getName().equals("writeObject") && parameterTypeList.size() == 1
                && parameterTypeList.get(0).getClassType().equals("java.io.ObjectOutputStream");
    }

    private boolean isEquals(MethodObject methodObject) {
        List<TypeObject> parameterTypeList = methodObject.getParameterTypeList();
        return methodObject.getName().equals("equals") && methodObject.getReturnType().getClassType().equals("boolean") &&
                parameterTypeList.size() == 1 && parameterTypeList.get(0).getClassType().equals("java.lang.Object");
    }

    private boolean isHashCode(MethodObject methodObject) {
        List<TypeObject> parameterTypeList = methodObject.getParameterTypeList();
        return methodObject.getName().equals("hashCode") && methodObject.getReturnType().getClassType().equals("int")
                && parameterTypeList.size() == 0;
    }

    private boolean isToString(MethodObject methodObject) {
        List<TypeObject> parameterTypeList = methodObject.getParameterTypeList();
        return methodObject.getName().equals("toString") && methodObject.getReturnType().getClassType().equals("java.lang.String")
                && parameterTypeList.size() == 0;
    }

    private boolean isClone(MethodObject methodObject) {
        List<TypeObject> parameterTypeList = methodObject.getParameterTypeList();
        return methodObject.getName().equals("clone") && methodObject.getReturnType().getClassType().equals("java.lang.Object")
                && parameterTypeList.size() == 0;
    }

    private boolean isCompareTo(MethodObject methodObject) {
        List<TypeObject> parameterTypeList = methodObject.getParameterTypeList();
        return methodObject.getName().equals("compareTo") && methodObject.getReturnType().getClassType().equals("int")
                && parameterTypeList.size() == 1;
    }

    private boolean containsFieldAccessOfEnclosingClass(MyMethod method) {
        return method.getMethodObject().containsFieldAccessOfEnclosingClass();
    }

    private boolean overridesMethod(MyMethod method) {
        return method.getMethodObject().overridesMethod();
    }

    private boolean containsSuperMethodInvocation(MyMethod method) {
        return method.getMethodObject().containsSuperMethodInvocation();
    }

    private boolean isSynchronized(MyMethod method) {
        return method.getMethodObject().isSynchronized();
    }

    @Override
    public Set<String> getEntitySet() {
        return sourceClass.getEntitySet();
    }

    public PsiClass getSourceClassTypeDeclaration() {
        return (PsiClass) sourceClass.getClassObject().getAbstractTypeDeclaration();
    }

    @Override
    public String getSource() {
        return sourceClass.getName();
    }

    @Override
    public String getSourceEntity() {
        return sourceClass.toString();
    }

    @Override
    public String getTarget() {
        return null;
    }

    public String toString() {
        return sourceClass.toString() + DELIMITER + extractedEntities.toString();
    }

    public int compareTo(ExtractClassCandidateRefactoring other) {
        if(this.refactoredWAN < other.refactoredWAN)
            return -1;
        else if(this.refactoredWAN == other.refactoredWAN)
            return 0;
        return 1;
    }

    public void findTopics() {
        List<String> codeElements = new ArrayList<>();
        for (Entity entity : this.extractedEntities) {
            if (entity instanceof MyAttribute) {
                MyAttribute attribute = (MyAttribute) entity;
                codeElements.add(attribute.getName());
            } else if (entity instanceof MyMethod) {
                MyMethod method = (MyMethod) entity;
                codeElements.add(method.getMethodName());
            }
        }
        this.topics = TopicFinder.findTopics(codeElements);
    }

    public List<String> getTopics() {
        return topics;
    }

    public int getDistinctSourceDependencies() {
        return visualizationData.getDistinctSourceDependencies();
    }

    public int getDistinctTargetDependencies() {
        return visualizationData.getDistinctTargetDependencies();
    }

    public PsiJavaFile getSourceFile() {
        return sourceClass.getClassObject().getPsiFile();
    }

    public double getWANOfEntities() {
        return WANOfEntities;
    }

    public void setWANOfEntities(double WANOfEntities) {
        this.WANOfEntities = WANOfEntities;
    }

    public double getRefactoredWAN() {
        return refactoredWAN;
    }

    public void setRefactoredWAN() {
        double refactoredWAN = 0;
        Iterator<MyClass> classIterator = system.getClassIterator();
        ArrayList<Entity> allEntities = new ArrayList<>();
        while (classIterator.hasNext()) {
            MyClass myClass = classIterator.next();
            ArrayList<Entity> entities = new ArrayList<>();
            entities.addAll(myClass.getAttributeList());
            entities.addAll(myClass.getMethodList());
            allEntities.addAll(entities);
        }
        classIterator = system.getClassIterator();
        while (classIterator.hasNext()) {
            MyClass myClass = classIterator.next();
            ArrayList<Entity> entities = new ArrayList<>();
            entities.addAll(myClass.getAttributeList());
            entities.addAll(myClass.getMethodList());
            if(myClass.getName().equals(sourceClass.getName())) {
                entities.removeAll(extractedEntities);
                refactoredWAN += (double) entities.size() / (double) allEntities.size() * (double) entities.size();
                continue;
            }
            refactoredWAN += (double) entities.size() / (double) allEntities.size() * (double) entities.size();
        }
        this.refactoredWAN = refactoredWAN;
    }

    public  double caculateWANOfEntities(){
        double WANOfEntities = 0;
        Iterator<MyClass> classIterator = system.getClassIterator();
        ArrayList<Entity> allEntities = new ArrayList<>();
        while (classIterator.hasNext()) {
            MyClass myClass = classIterator.next();
            ArrayList<Entity> entities = new ArrayList<>();
            entities.addAll(myClass.getAttributeList());
            entities.addAll(myClass.getMethodList());
            allEntities.addAll(entities);
        }
        classIterator = system.getClassIterator();
        while (classIterator.hasNext()) {
            MyClass myClass = classIterator.next();
            ArrayList<Entity> entities = new ArrayList<>();
            entities.addAll(myClass.getAttributeList());
            entities.addAll(myClass.getMethodList());
            WANOfEntities += (double) entities.size() / (double) allEntities.size() * (double) entities.size();
        }
        return WANOfEntities;
    }
}