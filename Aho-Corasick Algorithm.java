package ru.eltech.ahocorasick.alg;

import java.util.ArrayList;
import java.util.Collections;

/**
 * This class implements logic of Aho-Corasick algorithm.
 * It also serves as facade for Node and Bohr, compressing methods of the latter
 * into rather compact form. <br>
 * This class is not linked to any form of visual logic, thought visual enhancements
 * can be made using more advanced Bohr class. <br>
 */
public class Algorithm {
    /**
     * Default constructor
     * @param bohr Required Bohr object
     */
    public Algorithm(Bohr bohr){
        this(bohr, true);
    }

    public Algorithm(Bohr bohr, boolean historyOn){
        this.bohr = bohr;
        strings = new ArrayList<>();
        results = new ArrayList<>();
        text = "";
        if (history == null)
            if (historyOn)
                history = new AlgorithmHistory(100);
            else
                history = new AlgorithmHistory(0, false);
    }

    /**
     * This constructor creates Algorithm with a default Bohr
     */
    public Algorithm(){
        this(new Bohr());
    }

    /**
     * Returns current position in text
     */
    public int getTextPosition() {
        return textPosition;
    }

    /**
     * Resets whole Algorithm
     */
    public void reset(){
        if (bohr!=null)
            bohr.clear();
        history.clear();
        strings = new ArrayList<>();
        results = new ArrayList<>();
        text = "";
        textPosition = 0;
    }

    /**
     * Resets of algorithm status
     */
    public void restart(){
        history.save(this);
        textPosition = 0;
        bohr.clearTransitions();
        results.clear();

    }

    /**
     * Returns set text
     * @return String
     */
    public String getText() {
        return text;
    }

    /**
     * Returns ArrayList of results
     * @return ArrayList
     */
    public ArrayList<AlgorithmResult> getResults() {
        return processResults(results);
    }

    /**
     * Processes results to be more informative
     * @param results ArrayList of unprocessed results
     * @return ArrayList of processed results
     */
    private ArrayList<AlgorithmResult> processResults(ArrayList<AlgorithmResult> results) {
        ArrayList<AlgorithmResult> processed = new ArrayList<>(results.size());
        for (AlgorithmResult res : results){
            int delta = strings.get(res.getPatternNumber()).length() - 1;
            AlgorithmResult newRes = new AlgorithmResult(res.getIndex() - delta, res.getPatternNumber());
            processed.add(newRes);
        }
        processed.sort(new AlgorithmResult.AlgResComparator());
        return processed;
    }

    /**
     * Sets up text
     * @param text String
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Adds string to Bohr
     * @param str String
     */
    public void addString(String str){
        history.save(this);
        bohr.addString(str);
        strings.add(str);
    }

    public boolean doStep(){
        return doStep(true);
    }

    /**
     * Does source step
     * @return true if step was successful
     */
    private boolean doStep(boolean save){
        if (textPosition >= text.length())
            return false;
        if (save)
            history.save(this);
        bohr.getNextState(text.charAt(textPosition++));
        for (Node cur = bohr.getState(); cur != bohr.getRoot(); cur = bohr.getUp(cur)){
            if (cur.isLeaf()){
                for (int i : cur.getLeafPatternNumber()){
                    results.add(new AlgorithmResult(textPosition-1, i));
                }
            }
        }
        return true;
    }

    /**
     * Static methods which does all algorithm logic
     * @param text text to be processed
     * @param strings iterable list of strings
     * @return ArrayList of results
     */
    public static ArrayList<AlgorithmResult> doAhoCorasick(String text, Iterable<String> strings){
        Algorithm alg = new Algorithm(new Bohr(), false);
        for (String str : strings){
            alg.addString(str);
        }
        alg.setText(text);
        alg.finishAlgorithm();
        return alg.getResults();
    }

    /**
     * Finishes algorithm
     */
    public void finishAlgorithm() {
        boolean fin;
        do{
            fin = doStep(false);
        } while(fin);
    }

    @Override
    public String toString() {
        return bohr.toString() +
                "\ntextPosition = " + textPosition +
                "\ntext = " + text +
                "\n" + rawResultsToString() + "\nEND";

    }

    /**
     * Converts results array to more compact String
     * @return String of results in form [index:patternNumber]
     */
    private String rawResultsToString(){
        if (results == null)
            return "null";
        StringBuilder sb = new StringBuilder();
        for (AlgorithmResult res : results) {
            sb.append("[").append(res.getIndex()).append(":").append(res.getPatternNumber()).append("] ");
        }
        return sb.toString();
    }

    /**
     * Processes results and returns them in the String form
     * @return String of results in form [index:patternNumber]
     */
    public String resultsToString(){
        if (results == null)
            return "null";
        StringBuilder sb = new StringBuilder();
        for (AlgorithmResult res : processResults(results)) {
            sb.append("[").append(res.getIndex()).append(":").append(res.getPatternNumber()).append("] ");
        }
        return sb.toString();
    }

    /**
     * Converts string, received from rawResultsToString, to array of results
     * @param str string with results in form [index:patternNumber]
     * @return ArrayList of AlgorithmResult
     */
    private static ArrayList<AlgorithmResult> resultsFromString(String str){
        ArrayList<AlgorithmResult> res = new ArrayList<>();
        String[] arr = str.split("[:\\[\\]]");
        for (int i = 0; i < arr.length-2; i+=3){
            AlgorithmResult newRes = new AlgorithmResult(
                    Integer.valueOf(arr[i+1]),
                    Integer.valueOf(arr[i+2])
            );
            res.add(newRes);
        }
        return res;
    }

    /**
     * Converts String received from Algorithm.toString() to Algorithm.
     * This method can be used to save current state of Algorithm to a file
     * @param str String, generated by Algorithm.toString()
     * @return Algorithm
     */
    public static Algorithm fromString(String str){
        Algorithm alg;
        if (str.startsWith("BohrWithoutGraph")) {
            alg = new Algorithm(Bohr.fromString(str));
        }
        else {
            alg = new Algorithm(BohrWithGraph.fromString(str));
        }
        String[] arr = str.split("\n");
        try {
            alg.textPosition = Integer.valueOf(arr[arr.length - 4].substring(15));
        }
        catch (Exception e){
            alg.textPosition = 0;
        }
        try {
            alg.text = arr[arr.length - 3].substring(7);
        }
        catch (StringIndexOutOfBoundsException e){
            alg.text = "";
        }
        try {
            alg.results = resultsFromString(arr[arr.length - 2]);
        }
        catch (Exception e){
            alg.results = new ArrayList<>();
        }
        try {
            alg.strings = new ArrayList<>();
            Collections.addAll(alg.strings, alg.bohr.getStringArray());
        }
        catch (Exception e){
            alg.strings = new ArrayList<>();
        }
        return alg;
    }

    /**
     * Returns strings
     * @return ArrayList of Strings
     */
    public ArrayList<String> getStrings() {
        return strings;
    }

    /**
     * Used to help determine status of the Algorithm. <br>
     */
    public class Algorithm_Status{
        /**
         * Get status of Bohr. This method is quite expensive and so should not be overused
         * @return enum status {UNINITIALIZED, UNRESOLVED_DEPENDENCIES, CORRUPT_NODE, OK}
         */
        public Bohr.status getBohrStatus() {
            return bohr.getStatus();
        }

        /**
         * @return false, if TextPosition is not OK
         */
        public boolean isTextPositionOK() {
            return textPositionOK;
        }

        /**
         * @return false, if text is not OK
         */
        public boolean isTextOK() {
            return textOK;
        }

        /**
         * @return false, if results are not ok
         */
        public boolean isResultsOK() {
            return resultsOK;
        }

        /**
         * @return true, if everything is OK
         */
        public boolean isOK(){
            return ((bohr.getStatus() == Bohr.status.OK) && textPositionOK && textOK && resultsOK && stringsOK);
        }

        @Override
        public String toString() {
            if (isOK())
                return "OK";
            else {
                StringBuilder sb = new StringBuilder();
                if (!textPositionOK)
                    sb.append("TextPositionNotOK ");
                if (!textOK)
                    sb.append("TextNotOK ");
                if (!resultsOK)
                    sb.append("ResultsNotOK ");
                if (!stringsOK)
                    sb.append("StringsNotOK ");
                if (bohr.getStatus() != Bohr.status.OK)
                    sb.append("Bohr_").append(bohr.getStatus());
                return sb.toString();
            }
        }

        private boolean textPositionOK = true;
        private boolean textOK = true;
        private boolean resultsOK = true;
        private boolean stringsOK = true;
    }

    /**
     * Returns status of the Algorithm.
     * @see Algorithm_Status
     * @return class with status
     */
    public Algorithm_Status getStatus() {
        Algorithm_Status status = new Algorithm_Status();
        if (textPosition < 0)
            status.textPositionOK = false;
        if (text == null)
            status.textOK = false;
        if (results == null)
            status.resultsOK = false;
        if (strings == null)
            status.stringsOK = false;
        return status;
    }

    public AlgorithmHistory getHistory() {
        return history;
    }

    public Bohr getBohr() {
        return bohr;
    }

    private static AlgorithmHistory history;
    private final Bohr bohr;
    private ArrayList<String> strings;
    private ArrayList<AlgorithmResult> results;
    private String text;
    private int textPosition;
}
