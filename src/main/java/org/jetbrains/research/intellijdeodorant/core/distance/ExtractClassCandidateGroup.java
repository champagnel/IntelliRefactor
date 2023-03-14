package org.jetbrains.research.intellijdeodorant.core.distance;

import java.util.*;

public class ExtractClassCandidateGroup implements Comparable<ExtractClassCandidateGroup> {
    private final String source;
    private final ArrayList<ExtractClassCandidateRefactoring> candidates;
    private final ArrayList<ExtractedConcept> extractedConcepts;
    private double minWANOfEntities;

    public ExtractClassCandidateGroup(String source) {
        this.source = source;
        this.candidates = new ArrayList<>();
        this.extractedConcepts = new ArrayList<>();
    }

    public ArrayList<ExtractedConcept> getExtractedConcepts() {
        return extractedConcepts;
    }

    public String getSource() {
        return source;
    }

    public void addCandidate(ExtractClassCandidateRefactoring candidate) {
        this.candidates.add(candidate);
    }

    public ArrayList<ExtractClassCandidateRefactoring> getCandidates() {
        Collections.sort(candidates);
        return candidates;
    }

    public void groupConcepts() {
        ArrayList<ExtractClassCandidateRefactoring> tempCandidates = new ArrayList<>(candidates);
        tempCandidates.sort(new ClusterSizeComparator());
        while (!tempCandidates.isEmpty()) {
            Set<Entity> conceptEntities = new HashSet<>(tempCandidates.get(0).getExtractedEntities());
            Set<Integer> indexSet = new LinkedHashSet<>();
            indexSet.add(0);
            int previousSize;
            do {
                previousSize = conceptEntities.size();
                for (int i = 1; i < tempCandidates.size(); i++) {
                    HashSet<Entity> copiedConceptEntities = new HashSet<>(conceptEntities);
                    copiedConceptEntities.retainAll(tempCandidates.get(i).getExtractedEntities());
                    if (!copiedConceptEntities.isEmpty()) {
                        conceptEntities.addAll(tempCandidates.get(i).getExtractedEntities());
                        indexSet.add(i);
                    }
                }
            } while (previousSize < conceptEntities.size());
            Set<ExtractClassCandidateRefactoring> candidatesToBeRemoved = new HashSet<>();
            ExtractedConcept newConcept = new ExtractedConcept(conceptEntities);
            for (Integer j : indexSet) {
                newConcept.addConceptCluster(tempCandidates.get(j));
                candidatesToBeRemoved.add(tempCandidates.get(j));
            }
            tempCandidates.removeAll(candidatesToBeRemoved);
            extractedConcepts.add(newConcept);
        }
        findConceptTerms();
    }

    private void findConceptTerms() {
        for (ExtractedConcept concept : extractedConcepts) {
            concept.findTopics();
            for (ExtractClassCandidateRefactoring conceptCluster : concept.getConceptClusters()) {
                conceptCluster.findTopics();
            }
        }
    }

    public int compareTo(ExtractClassCandidateGroup other) {
        double thisNum = this.getMinWANOfEntities();
        double otherNum = other.getMinWANOfEntities();
        if(thisNum < otherNum)
            return -1;
        else if(thisNum == otherNum)
            return 0;
        return 1;
    }

    public double getMinWANOfEntities() {
        return minWANOfEntities;
    }

    public void setMinWANInThisGroup() {
        double min = Double.MAX_VALUE;
        for(ExtractClassCandidateRefactoring candidate : candidates) {
            if(candidate.getRefactoredWAN() < min) {
                min = candidate.getRefactoredWAN();
            }
        }
        this.minWANOfEntities = min;
    }
}
