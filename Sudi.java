import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/**
 * Class that uses Hidden Markov Models and Viterbi's algorithm to make predictions of parts of speech based
 * on given files and information
 * @author CJ Aggarwal, Fall 2020, November 6th
 */
public class Sudi {

    //instance variables
    public Map<String, Map<String, Double>> observations = new HashMap<String, Map<String, Double>>();
    public Map<String, Map<String, Double>> transitions = new HashMap<String, Map<String, Double>>();
    private double U = -100.0; //for an unseen word — value is instructed to be -100 in the problem set instructions

    /**
     * Viterbi's algorithm —  implemented in part based on Prof. Pierson's pseudocode
     * Provides the most likely path of parts of speech using back tracing
     * @param line the given sentence
     * @return the most likely path of parts of speech
     */
    public List<String> viterbi(String line) {
        //the following is an implementation of pseudo code provided by Prof. Pierson
        List<String> result = new ArrayList<>();
        List<Map<String, String>> backtrace = new ArrayList<>();

        String[] split = line.split(" ");
        Set<String> currStates = new HashSet<>();
        Map<String, Double> currScores = new HashMap<>();

        currStates.add("#");
        currScores.put("#", 0.0);

        for (int i = 0; i < split.length; i++) {
            Set<String> nextStates = new HashSet<>();
            Map<String, Double> nextScores = new HashMap<>();
            backtrace.add(new HashMap<>());

            for (String currState : currStates) {

                if (transitions.containsKey(currState)) {

                    for (String nextState : transitions.get(currState).keySet()) {
                        nextStates.add(nextState);

                        double obsScore;
                        if (!observations.get(nextState).containsKey(split[i])) {
                            obsScore = U;
                        } else {
                            obsScore = observations.get(nextState).get(split[i]);
                        }

                        double nextScore = currScores.get(currState) + transitions.get(currState).get(nextState) + obsScore;

                        if (!nextScores.containsKey(nextState) || (nextScores.containsKey(nextState) && nextScore > nextScores.get(nextState))) {
                            nextScores.put(nextState, nextScore);
                            backtrace.get(backtrace.size() - 1).remove(nextState);
                            backtrace.get(backtrace.size() - 1).put(nextState, currState);
                        }
                    }
                }
            }
            currStates = nextStates;
            currScores = nextScores;
        }

        //the rest is self-coded
        String currenthigh = null; //find the starting point, maxscore
        for (String state: currScores.keySet()) {
            if (currenthigh == null){
                currenthigh = state;
            }
            else {
                if (currScores.get(state) > currScores.get(currenthigh)) {
                    currenthigh = state;
                }
            }
        }
        result.add(currenthigh);
        for (int i = backtrace.size()-1; i>=0; i--){
            Map<String, String> currMap = backtrace.get(i);
            String prev = currMap.get(currenthigh);
            result.add(prev);
            currenthigh = prev;
        }
        List<String> answer = new ArrayList<String>();
        for (int i = result.size()-2; i>=0; i--){ //-2 to get rid of the "#"
            answer.add(result.get(i));
        }
        return answer;
    }

    /**
     * Creates nested maps which gathers normalized probabilities and trains based on tags, observations, and transitions
     * @param observation a hashmap of observations and their frequencies
     * @param state a hashmap of observations and their frequencies
     */
    public void training(String observation, String state){

        try{
            BufferedReader observationsReader = new BufferedReader(new FileReader(observation));
            BufferedReader stateReader = new BufferedReader(new FileReader(state));

            String currObs;
            String currState;
            while ((currObs = observationsReader.readLine()) != null && (currState = stateReader.readLine()) != null) {
                currObs = currObs.toLowerCase();
                String[] obsSplit = currObs.split(" ");
                String[] stateSplit = currState.split(" ");

                for (int i = 0; i < obsSplit.length; i++) {
                    if (!observations.containsKey(stateSplit[i])) { //check if not found
                        observations.put(stateSplit[i], new HashMap<>()); //add new if not
                    }
                    if (observations.get(stateSplit[i]).containsKey(obsSplit[i])) { //if found
                        observations.get(stateSplit[i]).put(obsSplit[i], observations.get(stateSplit[i]).get(obsSplit[i]) + 1.0); //add to double in nested map
                    }
                    else {
                        observations.get(stateSplit[i]).put(obsSplit[i], 1.0); //not seen this word as this part of speech
                    }
                }
                if (!transitions.containsKey("#")) { //starting point to maintain probabilities for start of sentence
                    transitions.put("#", new HashMap<>());
                }
                else {
                    if (transitions.get("#").containsKey(stateSplit[0])) {
                        transitions.get("#").put(stateSplit[0], transitions.get("#").get(stateSplit[0])+1.0);
                    }
                    else {
                        transitions.get("#").put(stateSplit[0], 1.0);
                    }
                }
                for (int i=0; i<stateSplit.length-1; i++) { //generally the same logic as above
                    if (!transitions.containsKey(stateSplit[i])) {
                        transitions.put(stateSplit[i], new HashMap<>());
                    }
                    if (transitions.get(stateSplit[i]).containsKey(stateSplit[i+1])) {
                        transitions.get(stateSplit[i]).put(stateSplit[i+1], transitions.get(stateSplit[i]).get(stateSplit[i+1])+1.0);
                    }
                    else {
                        transitions.get(stateSplit[i]).put(stateSplit[i+1], 1.0);
                    }
                }
            }
            for (String pos: transitions.keySet()) {
                double sumTrans = 0.0;
                for (String next: transitions.get(pos).keySet()) {
                    sumTrans += transitions.get(pos).get(next);
                }

                for (String next: transitions.get(pos).keySet()) {
                    transitions.get(pos).put(next, Math.log(transitions.get(pos).get(next)/sumTrans));
                }
            }
            for (String pos: observations.keySet()) {
                double sumObs = 0.0;
                for (String word: observations.get(pos).keySet()) {
                    sumObs += observations.get(pos).get(word);
                }

                for (String word: observations.get(pos).keySet()) {
                    observations.get(pos).put(word, Math.log(observations.get(pos).get(word)/sumObs));
                }
            }
            //convertLog(transitions);
            //convertLog(observations);
            observationsReader.close();
            stateReader.close();
        }
        catch (Exception e) {
            System.out.println("Unable to handle training files.");
        }
    }

    /**
     * A method to convert to log.
     * @param given a given map to be converted
     */
    public static void convertLog(Map<String, Map<String, Double>> given){
        for (String iterate: given.keySet()){ //same logic as above
            double sum = 0.0;
            for (String next: given.get(iterate).keySet()) {
                sum += given.get(iterate).get(next);
            }

            for (String next: given.get(sum).keySet()) {
                given.get(iterate).put(next, Math.log(given.get(iterate).get(next)/sum)); //given math function
            }
        }

    }

    /**
     * A console-based test that allows users to input their own sentences to be run using Sudi.
     */
    public void consoleBasedTest(){
        Scanner scan = new Scanner(System.in);
        System.out.println("This is a console-based test. Input your sentence or type stop to stop.");
        boolean stop = false; //boolean to ensure program can quit properly
        while (!stop){
            String nextLine = scan.nextLine();
            if (nextLine.equals("stop")){
                System.out.println("Stopping as requested.");
                stop = true;
            }
            else System.out.println(viterbi(nextLine)); //read and run viterbi
        }

    }

    /**
     * Calculates the actual accuracy of Sudi using given files and answers
     * @param sentenceName the sentences file name
     * @param tagName the tags file name
     */
    public void fileBasedTest(String sentenceName, String tagName){
        try{
            BufferedReader sentenceInput = new BufferedReader(new FileReader(sentenceName));
            BufferedReader tagInput = new BufferedReader(new FileReader(tagName));
            String sentence, tags;
            int total = 0, correct = 0;
            while ((sentence = sentenceInput.readLine()) != null && (tags = tagInput.readLine()) != null) { //iterate through
                sentence = sentence.toLowerCase();
                List<String> pred = viterbi(sentence);
                String[] array = tags.split(" ");
                if (pred.size() == array.length) {
                    total += array.length; //add to total count
                    for (int i=0; i<pred.size(); i++) {
                        if (pred.get(i).equals(array[i])) {
                            correct += 1; //add to correct count
                        }
                    }
                }
            }

            System.out.println("Sudi correctly predicted " + correct + " tags out of a total of " + total +
                    ".\nSudi also, unfortunately, incorrectly predicted " + (total-correct) + " tags."); //print results
            sentenceInput.close(); //try to close files
            tagInput.close();
        }
        catch (Exception e){
            System.out.println("Unable to handle testing files."); //exception handling
        }
    }

    /**
     * Hard codes observations and transitions to be as determined in lecture.
     * This is in accordance with the instructions of the problem set
     * to test if the Viterbi method works.
     */
    public void iceCreamSetUp() {
        Map<String, Double> beginning = new HashMap<String, Double>();
        beginning.put("hot", 5.); //given values
        beginning.put("cold", 5.);
        transitions.put("#", beginning);
        Map<String, Double> coldT = new HashMap<String, Double>();
        coldT.put("cold", 7.);//given values
        coldT.put("hot", 3.);
        Map<String, Double> hotT = new HashMap<String, Double>();
        hotT.put("cold", 3.);//given values
        hotT.put("hot", 7.);
        transitions.put("cold", coldT);
        transitions.put("hot", hotT);
        Map<String, Double> hotO = new HashMap<String, Double>();
        Map<String, Double> coldO = new HashMap<String, Double>();
        hotO.put("one", 2.);//given values
        hotO.put("two", 3.);
        hotO.put("three", 5.);//given values
        coldO.put("one", 7.);
        coldO.put("two", 2.);//given values
        coldO.put("three", 1.);
        observations.put("cold", coldO);
        observations.put("hot", hotO);
    }
    /**
     * The main function that calls the methods.
     * @param args
     */
    public static void main(String[] args) {
        //parts of hard-coded ice cream example
        //Sudi icecream = new Sudi();
        //icecream.iceCreamSetUp();
        //icecream.consoleBasedTest();

        Sudi sudi = new Sudi();
        System.out.println("Welcome! My name is Sudi, and I am here to predict the parts of speech of a given sentence!");
        sudi.training("inputs/simple-train-sentences.txt", "inputs/simple-train-tags.txt");
        //System.out.println(sudi.viterbi("the dog is fast ."));
        //System.out.println(sudi.viterbi("we should fast in a cave."));
        //System.out.println(sudi.viterbi("he trains the dog"));
        //System.out.println(sudi.viterbi("he fasts the dog"));
        //System.out.println(sudi.viterbi("he trains fast ."));
        //System.out.println(sudi.viterbi("we watch the cave"));
        //System.out.println(sudi.viterbi("the cat jumps fast")); //interesting
        //System.out.println(sudi.viterbi("the cat jumps high")); //interesting when compared to directly above
        //System.out.println(sudi.viterbi("we watch the watch in the cave")); //also interesting test case
        //System.out.println(sudi.viterbi("the dog saw trains in the night ."));
        //System.out.println(sudi.viterbi("my watch glows in the night ."));
        //System.out.println(sudi.viterbi("his work is to bark in a cave ."));
        //System.out.println(sudi.viterbi("i watch your dog for money ."));
        //System.out.println(sudi.viterbi("my dog trains to bark ."));
        sudi.consoleBasedTest();
        //sudi.training("inputs/brown-train-sentences.txt", "inputs/brown-train-tags.txt");
        //sudi.fileBasedTest("inputs/brown-test-sentences.txt", "inputs/brown-test-tags.txt");
    }
}
