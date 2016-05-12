/**
 * Copyright 2016 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.wordcloud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractWordFrequencyProvider implements IWordFrequencyProvider 
{
	// sorts descending by word frequency
	public static final Comparator<Entry<String, Integer>> WORD_COMPARATOR  = (a,b) -> b.getValue().compareTo( a.getValue() );

	private static final Set<String> STOP_WORDS;

	static 
	{
		final String[] words = { "a","able","about","above","according","accordingly","across","actually","after","afterwards",
				"again","against","ain't","all","allow","allows","almost","alone","along","already",
				"also","although","always","am","among","amongst","an","and","another","any",
				"anybody","anyhow","anyone","anything","anyway","anyways","anywhere","apart","appear","appreciate",
				"appropriate","are","aren't","around","as","a's","aside","ask","asking","associated",
				"at","available","away","awfully","b","be","became","because","become","becomes",
				"becoming","been","before","beforehand","behind","being","believe","below","beside","besides",
				"best","better","between","beyond","both","brief","but","by","c","came",
				"can","cannot","cant","can't","cause","causes","certain","certainly","changes","clearly",
				"c'mon","co","com","come","comes","concerning","consequently","consider","considering","contain",
				"containing","contains","corresponding","could","couldn't","course","c's","currently","d","definitely",
				"described","despite","did","didn't","different","do","does","doesn't","doing","done",
				"don't","down","downwards","during","e","each","edu","eg","eight","either",
				"else","elsewhere","enough","entirely","especially","et","etc","even","ever","every",
				"everybody","everyone","everything","everywhere","ex","exactly","example","except","f","far",
				"few","fifth","first","five","followed","following","follows","for","former","formerly",
				"forth","four","from","further","furthermore","g","get","gets","getting","given",
				"gives","go","goes","going","gone","got","gotten","greetings","h","had",
				"hadn't","happens","hardly","has","hasn't","have","haven't","having","he","hello",
				"help","hence","her","here","hereafter","hereby","herein","here's","hereupon","hers",
				"herself","he's","hi","him","himself","his","hither","hopefully","how","howbeit",
				"however","i","i'd","ie","if","ignored","i'll","i'm","immediate","in",
				"inasmuch","inc","indeed","indicate","indicated","indicates","inner","insofar","instead","into",
				"inward","is","isn't","it","it'd","it'll","its","it's","itself","i've",
				"j","just","k","keep","keeps","kept","know","known","knows","l",
				"last","lately","later","latter","latterly","least","less","lest","let","let's",
				"like","liked","likely","little","look","looking","looks","ltd","m","mainly",
				"many","may","maybe","me","mean","meanwhile","merely","might","more","moreover",
				"most","mostly","much","must","my","myself","n","name","namely","nd",
				"near","nearly","necessary","need","needs","neither","never","nevertheless","new","next",
				"nine","no","nobody","non","none","noone","nor","normally","not","nothing",
				"novel","now","nowhere","o","obviously","of","off","often","oh","ok",
				"okay","old","on","once","one","ones","only","onto","or","other",
				"others","otherwise","ought","our","ours","ourselves","out","outside","over","overall",
				"own","p","particular","particularly","per","perhaps","placed","please","plus","possible",
				"presumably","probably","provides","q","que","quite","qv","r","rather","rd",
				"re","really","reasonably","regarding","regardless","regards","relatively","respectively","right","s",
				"said","same","saw","say","saying","says","second","secondly","see","seeing",
				"seem","seemed","seeming","seems","seen","self","selves","sensible","sent","serious",
				"seriously","seven","several","shall","she","should","shouldn't","since","six","so",
				"some","somebody","somehow","someone","something","sometime","sometimes","somewhat","somewhere","soon",
				"sorry","specified","specify","specifying","still","sub","such","sup","sure","t",
				"take","taken","tell","tends","th","than","thank","thanks","thanx","that",
				"thats","that's","the","their","theirs","them","themselves","then","thence","there",
				"thereafter","thereby","therefore","therein","theres","there's","thereupon","these","they","they'd",
				"they'll","they're","they've","think","third","this","thorough","thoroughly","those","though",
				"three","through","throughout","thru","thus","to","together","too","took","toward",
				"towards","tried","tries","truly","try","trying","t's","twice","two","u",
				"un","under","unfortunately","unless","unlikely","until","unto","up","upon","us",
				"use","used","useful","uses","using","usually","uucp","v","value","various",
				"very","via","viz","vs","w","want","wants","was","wasn't","way",
				"we","we'd","welcome","well","we'll","went","were","we're","weren't","we've",
				"what","whatever","what's","when","whence","whenever","where","whereafter","whereas","whereby",
				"wherein","where's","whereupon","wherever","whether","which","while","whither","who","whoever",
				"whole","whom","who's","whose","why","will","willing","wish","with","within",
				"without","wonder","won't","would","wouldn't","x","y","yes","yet","you",
				"you'd","you'll","your","you're","yours","yourself","yourselves","you've","z","zero"};

			STOP_WORDS = Arrays.stream( words ).map( String::toLowerCase ).collect( Collectors.toSet() );
	}

	private Map<String, Integer> wordCounts;

	private int totalWordCount;

	protected abstract Iterator<String> getWords();

	private boolean initialized;

	private void init() 
	{
		if ( initialized ) {
			return;
		}
		this.wordCounts = new HashMap<>();
		int wordCount = 0;
		final Iterator<String> it = getWords();
		while ( it.hasNext() ) 
		{
			final String word = it.next().toLowerCase();
			if ( STOP_WORDS.contains( word ) ) 
			{
				continue;
			}
			Integer existing = wordCounts.get( word );
			if ( existing == null ) {
				wordCounts.put( word , Integer.valueOf( 1 ) );
			} else {
				wordCounts.put( word , Integer.valueOf( existing.intValue() + 1 ) );
			}
			wordCount++;
		}
		this.totalWordCount = wordCount;
		initialized = true;
	}

	@Override
	public Map<String, Integer> getWordCounts(int limit) 
	{
		init();

		final List<Map.Entry<String,Integer>> counts = new ArrayList<>( wordCounts.entrySet() );
		counts.sort( WORD_COMPARATOR );
		final Map<String, Integer> result = new HashMap<>();
		final Iterator<Map.Entry<String,Integer>> it = counts.iterator();
		for ( int i = 0 ; i < limit && it.hasNext() ; i++ ) 
		{
			final Entry<String, Integer> entry = it.next();
			result.put( entry.getKey() , entry.getValue() );
		}
		return result;
	}

	@Override
	public int getTotalWordCount() {
		init();
		return totalWordCount;
	}
}