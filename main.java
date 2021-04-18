import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class main {

    static final int PRICE_SIG_FIG = 2;
    static final boolean TABLE = true;
    static final boolean PAD = true;

    public static void main(String args[]) throws IOException, InterruptedException {

        //check inputs
        if (args.length!=2){
            System.out.println("Please only input two arguments.");
            System.exit(0);
        }

        // start with the inputs
        // there should be two numerical args, "duration" and "interval"
        // are they ints? maybe floats?
        // i'm going to go with floats because maybe you want a fraction of a second for interval

        float duration = Float.parseFloat(args[0]);
        float interval = Float.parseFloat(args[1]);


        // what do we do after we have the inputs
        // think of what else we want to do and create helper methods
        // first thing i want to do is be able to get info from https://api.coindesk.com/v1/bpi/currentprice.json
        // create the helper method that gets JSON
        long startTime = System.currentTimeMillis();
        String info = getURLInfo("https://api.coindesk.com/v1/bpi/currentprice.json");

        // now that we've got all the info time to use regex magic in another helper function
        // I mean I could try and parse the JSON  but I forgot all of that and it would take a while to relearn.
        HashMap<String, Float> prices = getBitcoinPrices(info);

        // now we have the bitcoin prices in a hashmap
        // its a bit inefficient since we're just going to remove it from the hashmap to format and print
        // but it feels nice to have data like that in a hashmap
        // time to build a helper function to print the data we've just got
        if(!TABLE){
            System.out.println(prices);
        }else {
            printBitcoinPrices(prices);
        }

        //lets take runTime into account.
        long endTime = System.currentTimeMillis();
        long runTime = endTime-startTime;

        // Now that we've built printBitcoinPrices it's time to put it all in a loop to finish off
        // the first iteration is already done above. One assumption is that when called you want to bookend
        // the program with bitcoin prices. i.e: one at the start and one at the end.

        //intervals of 1 don't work with my bookend assumption so just run once and end.
        if (interval <= 1){
            System.exit(0);
        }

        // calc how long to sleep between each iteration.
        // subtracting runtime isn't anywhere close to good but it might bet better than just completely ignoring it
        // there will be much better and more elegant solutions but it's just pedantics for a small test.
        // interval-1 takes into account bookending.
        long sleepTime = (long)((duration/(interval-1))*1000) - runTime;

        // simple sleep and loop
        for(int i = 1; i < interval; i ++){
            Thread.sleep(sleepTime);
            info = getURLInfo("https://api.coindesk.com/v1/bpi/currentprice.json");
            prices = getBitcoinPrices(info);

            System.out.println("");
            if(!TABLE){
                System.out.println(prices);
            }else{
                printBitcoinPrices(prices);
            }
        }
    }


    public static void printBitcoinPrices(HashMap<String, Float> pricesMap){

        ArrayList<String> currencies = new ArrayList<String>(pricesMap.keySet());
        ArrayList<String> prices = new ArrayList<String>();
        ArrayList<String> finalPrices = prices; //if we change prices, next line breaks so using a temp final copy
        // honestly, lambda functions in java confuse me.
        (pricesMap.values()).forEach((price) -> finalPrices.add(price.toString()));

        //just adding headers and padding
        currencies.add(0, "Currency");
        prices.add(0, "Price");
        currencies = padStrings(currencies);
        prices = padStrings(prices);

        // printing in a pseudo table format
        // I was using | at the start and the end but right at the end I saw that you had to put currency symbols
        for (int i = 0; i<currencies.size(); i++){
            if (i==0){ //print headers
                System.out.print(currencies.get(i));
                System.out.println(" | " + prices.get(i));
            }else{ //get currency symbol and print with that
                Currency cur = Currency.getInstance(currencies.get(i).replaceAll(" ",""));
                String symbol = cur.getSymbol();
                System.out.print(currencies.get(i));
                System.out.println(" | " + prices.get(i) + " "  + symbol);
            }
        }
    }


    // Padding seems a bit redundant since all currencies are 3 chars long and all prices brought down to 2 sig figs are 9
    // chars long but bitcoin fluctuates a decent amount and while future proofing this seems silly considering how I
    // did the regex, why not do it for the fun. Oh yea, also it allows us to add headers.
    // pads strings in an arrayList with spaces to the left so they're all the same length.
    public static ArrayList<String> padStrings(ArrayList<String> stringArrayList){

        // get the length of the longest string in the arraylist
        int longestLen = stringArrayList.get(0).length();
        for (String strng : stringArrayList) {
            if (strng.length() > longestLen) {
                longestLen = strng.length();
            }
        }

        // if any string is shorter than the longest then pad it out with spaces using formatting
        for (int i = 0; i<stringArrayList.size(); i++){
            String tempString = stringArrayList.get(i);
            if (tempString.length() < longestLen){
                String padding = "%-" + longestLen + "s";
                stringArrayList.set(i,String.format(padding,tempString));
            }
        }

        return stringArrayList;
    }

    // Take info and use regex to get a hashmap of bitcoin prices in different currencies.
    // The key being the currency code and the value being the price.
    public static HashMap<String, Float> getBitcoinPrices(String info){
        HashMap<String, Float> prices = new HashMap<String, Float>();

        //regex pattern capturing groups, i.e stuff surrounded by () brackets.
        Pattern infoPattern = Pattern.compile("\"code\":\"(.*?)\",\"symbol\":\".*?\",\"rate\":\"(.*?)\"");
        Matcher match = infoPattern.matcher(info);
        while (match.find()){
            // currency = USD, GBP, etc...
            String currency = match.group(1);

            // replace , in the number with nothing and convert to float
            float price = Float.parseFloat(match.group(2).replaceAll(",",""));

            // rounding to 2 sig figs
            float multiplier = (float)Math.pow(10,PRICE_SIG_FIG);
            price = ((float)Math.round(price*multiplier))/multiplier;

            // putting data into hashmap
            prices.put(currency, price);
        }

        return prices;
    }


    public static String getURLInfo(String url) throws IOException {
        URL infoURL = new URL(url);
        InputStream infoStream = infoURL.openStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(infoStream));
        String info = br.readLine();
        //always remember to close stream (which is why not to return br.readline)
        infoStream.close();
        return info;
    }

}