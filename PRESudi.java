import java.util.HashMap;
import java.util.List;

/**
 * Sudi.
 * @author Luke Wilbur
 */
public class PRESudi {
    String[] determinants = {"adj", "adv", "cnj", "det", "ex", "fw", "mod", "n", "np", "num", "pro", "p", "to", "uh", "v", "vd", "vg", "vn", "wh"};
    HashMap<String, HashMap<String, Double>> transitions;
    HashMap<String, HashMap<String, Double>> wordAppearances;


    public PRESudi() {
        transitions     = new HashMap<String, HashMap<String, Double>>();
        wordAppearances = new HashMap<String, HashMap<String, Double>>();
        createStates();

        // Setting up wordAppearances so that it can be used properly to track words as they appear
        for (String det : determinants)
            wordAppearances.put(det, new HashMap<String, Double>());  //TODO FIND OUT IF -3 MAKES ANY FUCKING SENSE WHATSOEVER

        trainStates("textFiles/texts/simple-train-sentences.txt",
                    "textFiles/texts/simple-train-tags.txt");

        // DEBUG
        System.out.println(transitions.toString().replace("=-Infinity", "=-I"));
        System.out.println(wordAppearances);
    }


    public void createStates() {
        // Setting up HashMap for states, separate HashMap to record how many times a state is transitioned from to another state
        for (String det : determinants) {
            transitions.put(det, new HashMap<String, Double>());
            for (String det2 : determinants) {
                transitions.get(det).put(det2, 0.0);
            }
        }
    }


    public void trainStates(String trainingSentencesFilepath, String trainingDeterminersFilepath) {
        List<String[]> sentences = FileRead.makeWordList(trainingSentencesFilepath);
        List<String[]> determiners = FileRead.makeWordList(trainingDeterminersFilepath);

        HashMap<String, HashMap<String, Double>> transitionTracker = (HashMap<String, HashMap<String, Double>>) transitions.clone();
        HashMap<String, Integer> detAppearances = new HashMap<String, Integer>();

        // TODO EITHER REMOVE ENTIRELY OR MAKE THE OTHER ARRAYS USED ALSO BE CLEARED OUT HERE
        // Reset values of hashMaps to be used, in case trainStates has been performed with another file previously
        for (String state : determinants) {
            detAppearances.put(state, 0);
            for (String state2 : determinants) {
                transitionTracker.get(state).put(state2, 0.0);
            }
        }

        // Go through each word/det pair in each sentence, record transition that occurs, what determiners appear in what amounts
        int numWords = 0;
        for (int sentence = 0; sentence < sentences.size(); sentence++) {
            for (int word = 0; word < sentences.get(sentence).length - 1; word++) {  // Go up to 2nd to last when comparing to avoid indexOutOfBounds
                // Pulling determiners and words out of lists
                String currDet = determiners.get(sentence)[word];
                String nextDet = determiners.get(sentence)[word + 1];
                String currWord = sentences.get(sentence)[word];
                String nextWord = sentences.get(sentence)[word + 1];

                // Keeping track of appearances of words by associated determinant
                //TODO deal with when word is the last in the sentence <-- this is why the current word counting doesn't work (I think)
                if (wordAppearances.get(currDet).containsKey(currWord))
                    wordAppearances.get(currDet).put(currWord, wordAppearances.get(currDet).get(currWord) + 1);
                else
                    wordAppearances.get(currDet).put(currWord, 1.0);

                // Keeping track of appearances of determinants
                transitionTracker.get(currDet).put(nextDet, transitionTracker.get(currDet).get(nextDet) + 1); // Update # transitions from det to nextDet
                detAppearances.put(currDet, detAppearances.get(currDet) + 1); // Update # appearances of det
            }
        }

        // Calculate values for the likelihood of each word appearing for each determinant, overwrite within wordAppearances
        for (String determinant : wordAppearances.keySet()) {
            numWords = sumUpNumWords(determinant);
            for (String word : wordAppearances.get(determinant).keySet()) {
                // Overwriting integer count with log of fraction of number of words for that determinant
                wordAppearances.get(determinant).put(word, Math.log10(wordAppearances.get(determinant).get(word) / (double)numWords));
            }
        }

        // Calculate values for the likelihood of each transition, put it into the transitions map
        for (String state : determinants) {
            int numStateAppearances = detAppearances.get(state); // Number of times determiner (state) was transitioned from
            for (String state2 : determinants) {
                if (numStateAppearances != 0) {  // Avoid division by 0 if determiner didn't appear in sentence
                    double numS1S2trans = transitionTracker.get(state).get(state2);  // Number of transitions from state to state 2
                    if(numS1S2trans != 0) {
                        System.out.print(state + " apps = " + numStateAppearances + ", # trans to " + state2 + " = " + numS1S2trans);
                        System.out.println("\t" + Math.log10(numS1S2trans / numStateAppearances));
                    }
                    transitions.get(state).put(state2, Math.log10(numS1S2trans / numStateAppearances));
                }
                else {  // If didn't appear, all transitions of that type should be -Infinity
                    transitions.get(state).put(state2, Math.log10(0));
                }
            }
        }
    }

    public int sumUpNumWords(String determinant) {
        int numWords = 0;
        for (String word : wordAppearances.get(determinant).keySet()) {
            numWords += wordAppearances.get(determinant).get(word);
        }

        return numWords;
    }


    public static void main(String[] args) {
        List<String[]> words = FileRead.makeWordList("textFiles/texts/simple-train-sentences.txt");
        List<String[]> tags  = FileRead.makeWordList("textFiles/texts/simple-train-tags.txt");
        //for (String[] word : words)
            //System.out.print(Arrays.toString(word) + " ");
        System.out.println();
       // for (String[] tag : tags)
            //System.out.print(Arrays.toString(tag) + " ");

        PRESudi s = new PRESudi();
    }
}
