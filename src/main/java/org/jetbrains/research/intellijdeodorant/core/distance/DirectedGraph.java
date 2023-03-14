package org.jetbrains.research.intellijdeodorant.core.distance;

import org.jetbrains.research.intellijdeodorant.core.ast.ClassObject;
import org.jetbrains.research.intellijdeodorant.core.ast.MethodObject;
import org.jetbrains.research.intellijdeodorant.core.ast.association.Association;

import java.util.*;

/**
 * @Auhtor Eric
 */
public class DirectedGraph {
    private final List<ClassObject> classObjectList;
    private final List<MyClass> classList;
    private final MySystem system;

    public DirectedGraph(MySystem system) {
        this.system = system;
        classObjectList = new ArrayList<>();
        classList = new ArrayList<>();
        generateGraph();
    }

    private void generateGraph() {
        Iterator<MyClass> classIt = system.getClassIterator();
        while (classIt.hasNext()) {
            MyClass myClass = classIt.next();
            classList.add(myClass);
            classObjectList.add(myClass.getClassObject());
        }

    }

    public double[][] getDirectedGraphMatrix(MySystem system) {
        double[][] directedGraphMatrix = new double[classObjectList.size()][classObjectList.size()];
        Map<Integer, String> indexToName = new HashMap<>();
        for(int i = 0; i < classObjectList.size(); ++i){
            ClassObject myClass = classObjectList.get(i);
            List<Association> associationsOfClass = system.getAssociationsOfClass(myClass);
            for(int j = 0; j < associationsOfClass.size(); ++j){
                Association association = associationsOfClass.get(j);
                for(int k = 0; k < classObjectList.size(); ++k){
                    if(association.getTo().equals(classObjectList.get(k).getName())){
                        directedGraphMatrix[i][k] += 1;
                    }
                }
            }
            String tempName = myClass.getName();
            int index = tempName.lastIndexOf(".");
            String className = tempName.substring(index+1);
            indexToName.put(i, className);
        }
        for(int i = 0; i < directedGraphMatrix.length; ++i){
            for(int j = 0; j < directedGraphMatrix[0].length; ++j){
                if(i != j && directedGraphMatrix[i][j] == 0){
                    directedGraphMatrix[i][j] = Double.MAX_VALUE;
                }
            }
        }
        int size= classObjectList.size();
        StringBuilder graph=new StringBuilder("digraph g {");

        for(int i=0;i<size;i++) {
            graph.append(i+"[label=\""+ classObjectList.get(i).getName()+"\"];");
        }

        for(int i=0;i<size;i++) {
            for(int j=0;j<size;j++) {
                if(directedGraphMatrix[i][j]!=Double.MAX_VALUE&&directedGraphMatrix[i][j]!=0) {
                    graph.append(i+"->"+j+"[label=\""+directedGraphMatrix[i][j]+"\"];");
                }
            }
        }
        graph.append("}");
        String html = WriteHtml.generateHTML(graph.toString());
        String file = "D:/DirectedGraph.html";
        WriteHtml.writeHTML(file, html);

        /**try {
            FileOutputStream fileOutputStream = new FileOutputStream("D:/DirectedGraph.html");
            PrintStream printStream = new PrintStream(fileOutputStream);
            StringBuilder sb = new StringBuilder();
            sb.append("<html lang=\"en\">\n");
            sb.append("<head>");
            sb.append("<meta charset=\"UTF-8\" />");
            sb.append("<title>Demo</title>");
            sb.append("</head>");
            sb.append("<body>");
            sb.append("<div id=\"mountNode\"></div>");
            sb.append("<script src=\"https://gw.alipayobjects.com/os/lib/antv/g6/4.3.11/dist/g6.min.js\"></script>\n");
            sb.append("<script>");
            sb.append("const data = {");
            sb.append("nodes: [");
            for(int i = 0; i < classObjectList.size(); ++i){
                sb.append("{");
                sb.append("id: '" + indexToName.get(i) + "',");
                //sb.append("label: '" + indexToName.get(i) + "',");
                sb.append("},");
            }
            sb.append("],");
            sb.append("edges: [");
            for (int i = 0; i < classObjectList.size(); ++i) {
                String sourceClass = indexToName.get(i);
                for (int j = 0; j < directedGraphMatrix[i].length; ++j) {
                    if(directedGraphMatrix[i][j] != 0 && directedGraphMatrix[i][j] != Double.MAX_VALUE) {
                        String targetClass = indexToName.get(j);
                        sb.append("{");
                        sb.append("source: '" + sourceClass + "',");
                        sb.append("target: '" + targetClass + "',");
                        sb.append("label: '" + String.valueOf(directedGraphMatrix[i][j]) + "',");
                        sb.append("style: {");
                        sb.append("endArrow: 'true',");
                        sb.append("},");
                        sb.append("},");
                    }
                }
            }
            sb.append("],");
            sb.append("};");
            sb.append("const graph = new G6.Graph({");
            sb.append("container: 'mountNode', ");
            sb.append("width: 800,");
            sb.append("height: 500,");
            sb.append("fitView: 'true'");
            sb.append("});");
            sb.append("graph.data(data);");
            sb.append("graph.render();");
            sb.append("</script>");
            sb.append("</body>");
            sb.append("</html>");
            printStream.println(sb.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/
        
        return directedGraphMatrix;
    }


    public double[] calculateDegreeCentralities(){
        double[][] directedGraphMatrix = getDirectedGraphMatrix(system);
        double[] degreeCentralities = new double[classObjectList.size()];
        for(int i = 0; i < directedGraphMatrix.length; ++i){
            for(int j = 0; j < directedGraphMatrix[0].length; ++j){
                if(directedGraphMatrix[i][j] < Double.MAX_VALUE){
                    degreeCentralities[j] += directedGraphMatrix[i][j];
                    degreeCentralities[i] += directedGraphMatrix[i][j];
                }
            }
        }
        if(degreeCentralities.length > 1) {
            for(int i = 0; i < directedGraphMatrix.length; ++i){
                degreeCentralities[i] = degreeCentralities[i] / (directedGraphMatrix.length - 1);
            }
        }
        return degreeCentralities;
    }

    public double[] calculateOutdegreeCentrality(){
        double[][] directedGraphMatrix = getDirectedGraphMatrix(system);
        double[] outDegreeCentralities = new double[classObjectList.size()];
        for(int i = 0; i < directedGraphMatrix.length; ++i){
            for(int j = 0; j < directedGraphMatrix[0].length; ++j){
                if(directedGraphMatrix[i][j] < Double.MAX_VALUE){
                    outDegreeCentralities[i] += directedGraphMatrix[i][j];
                }
            }
        }
        return outDegreeCentralities;
    }

    public List<ClassObject> collectClassWithGodClassProblem(){
        double[] degreeCentralities = calculateDegreeCentralities();
        List<ClassObject> classesWithGodClassProblem = new ArrayList<>();
        if(degreeCentralities.length == 1){
            classesWithGodClassProblem.add(classObjectList.get(0));
            return classesWithGodClassProblem;
        }
        double sum = 0;
        for(double degreeCentrality: degreeCentralities){
            sum += degreeCentrality;
        }
        double avg = sum / degreeCentralities.length;

        for(int i = 0; i < degreeCentralities.length; ++i){
            double degreeCentrality = degreeCentralities[i];
            if(avg > 0.0 && degreeCentrality > avg && ((degreeCentrality - avg) / avg >= 0.1)){
                classesWithGodClassProblem.add(classObjectList.get(i));
            }
        }
        return classesWithGodClassProblem;
    }

    public List<ClassObject> collectClassWithFeatureEnvyProblem(){
        double[] outDegreeCentralities = calculateOutdegreeCentrality();
        List<ClassObject> classesWithFeatureEnvyProblem = new ArrayList<>();

        double sum = 0;
        for(double outDegreeCentrality: outDegreeCentralities){
            sum += outDegreeCentrality;
        }
        double avg = sum / outDegreeCentralities.length;

        for(int i = 0; i < outDegreeCentralities.length; ++i){
            double outDegreeCentrality = outDegreeCentralities[i];
            if(avg > 0.0 && outDegreeCentrality > avg && ((outDegreeCentrality - avg) / avg >= 0.1)){
                classesWithFeatureEnvyProblem.add(classObjectList.get(i));
            }
        }
        return classesWithFeatureEnvyProblem;
    }

    public double caculateEntityPlacement(DistanceMatrix distanceMatrix, MyClass myClass, ArrayList<Entity> entities) {
        /*double[][] jaccardDistanceMatrix = distanceMatrix.getJaccardDistanceMatrix(myClass);
        double sum = 0.0;
        for(int i = 0; i < jaccardDistanceMatrix.length-1; ++i){
            for(int j = i+1; j < jaccardDistanceMatrix[0].length; ++j){
                sum += jaccardDistanceMatrix[i][j];
            }
        }**/
        double num1 = 0.0D;
        ArrayList<Entity> temp = new ArrayList<>(entities);
        for (Entity entity : entities) {
            temp.remove(entity);
            Set<String> s = new HashSet<>();
            for (Entity e : temp) {
                s.addAll(e.getFullEntitySet());
            }
            num1 += DistanceCalculator.getDistance(entity.getFullEntitySet(), s);
        }

        if (entities.size() != 0) {
            num1 = num1 / (double)entities.size();
        }
        double num2 = distanceMatrix.getJaccardDistanceMatrixFromSourceToOthers(myClass);
        if(num1 == 0) {
            return 1.0D;
        }
        double entityPlacement = num1 / num2;
        return entityPlacement;
    }

    public double caculateEntityPlacementForFeatureEnvy(DistanceMatrix distanceMatrix, MyClass myClass, MyClass sourceClass, MyMethod sourceMethod,List<Entity> entities) {
        double[][] jaccardDistanceMatrix = distanceMatrix.getJaccardDistanceMatrixForFeatureEnvy(entities, myClass);
        double sum = 0;
        for(int i = 0; i < jaccardDistanceMatrix.length-1; ++i){
            for(int j = i+1; j < jaccardDistanceMatrix[0].length; ++j){
                sum += jaccardDistanceMatrix[i][j];
            }
        }
        double num1 = sum / (double) jaccardDistanceMatrix.length;
        double num2 = distanceMatrix.getJaccardDistanceMatrixFromSourceToOthersForFeatureEnvy(myClass, sourceClass, sourceMethod,entities);
        double entityPlacement = num1 / num2;
        return entityPlacement;
    }

    public double caculateEntityPlacementOfSystem(){
        double entityPlacementOfSystem = 0.0D;
        List<Entity> allEntities = new ArrayList<>();
        for (MyClass myClass : classList){
            allEntities.addAll(myClass.getAttributeList());
            allEntities.addAll(myClass.getMethodList());
        }
        for (MyClass myClass : classList){
            ArrayList<Entity> entities = new ArrayList<>();
            entities.addAll(myClass.getAttributeList());
            entities.addAll(myClass.getMethodList());
            DistanceMatrix distanceMatrix = new DistanceMatrix(system);
            double entityPlacement = caculateEntityPlacement(distanceMatrix, myClass, entities);
            entityPlacementOfSystem += ((double) entities.size() / (double) allEntities.size()) * entityPlacement;
        }
        return entityPlacementOfSystem;
    }

    public double caculateFeatureEnvyEntityPlacementOfSystem(MyClass sourceClass, MyClass targetClass,
                                                             MyMethod sourceMethod,
                                                             List<MethodObject> additionalMethods){
        double result = 0;
        List<Entity> allEntities = new ArrayList<>();
        for (MyClass myClass : classList){
            allEntities.addAll(myClass.getAttributeList());
            allEntities.addAll(myClass.getMethodList());
        }

        List<MyMethod> temp = new ArrayList<>();
        for (MyClass myClass : classList){
            if(myClass.getName().equals(sourceClass.getName())){
                List<MyMethod> methodList = myClass.getMethodList();
                for(MyMethod method : methodList) {
                    if(additionalMethods.contains(method.getMethodObject())){
                        temp.add(method);
                    }
                }
                break;
            }
        }

        for (MyClass myClass : classList){
            ArrayList<Entity> entities = new ArrayList<>();
            entities.addAll(myClass.getAttributeList());
            List<MyMethod> list1 = myClass.getMethodList();
            List<MyMethod> methodList = new ArrayList<>(list1);
            if(myClass.getName().equals(sourceClass.getName())){
                methodList.remove(sourceMethod);
                if(temp.size() != 0) {
                    methodList.removeAll(temp);
                }
            }
            if(myClass.getName().equals(targetClass.getName())) {
                methodList.add(sourceMethod);
                if(temp.size() != 0) {
                    methodList.addAll(temp);
                }
            }
            entities.addAll(methodList);
            DistanceMatrix distanceMatrix = new DistanceMatrix(system);
            double EntityPlacement = caculateEntityPlacementForFeatureEnvy(distanceMatrix, myClass, sourceClass, sourceMethod, entities);
            result += (double) entities.size() / (double) allEntities.size() * EntityPlacement;
        }
        return result;
    }

}
