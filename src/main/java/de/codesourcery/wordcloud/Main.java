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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Main 
{
	public static final int WORDS_TO_DISCARD = 10;
	
	public static final int WORDS_TO_RENDER = 100;

	public static final float MAX_FONT_SIZE = 64;
	public static final float MIN_FONT_SIZE = 8;

	public static final Dimension IMAGE_SIZE = new Dimension(1024,768);

	private final int[] sumImage = new int[ IMAGE_SIZE.width * IMAGE_SIZE.height ];

	private BufferedImage image;
	private Graphics2D graphics;

	private final ThreadLocalRandom rnd = ThreadLocalRandom.current();

	public static final class WordEntry implements Comparable<WordEntry> 
	{
		private final String word;
		public float percentage;
		public final int count;

		public WordEntry(Pair pair,int totalCount) 
		{
			this.word = pair.word;
			this.count = pair.count;
			this.percentage = count / (float) totalCount;
		}

		@Override
		public int compareTo(WordEntry o) 
		{
			return Float.compare( o.percentage , this.percentage ); // sort descending
		}

		@Override
		public String toString() {
			return "'"+word+"' => "+percentage+" %";
		}
	}

	private static String getText(File file) throws IOException 
	{
		final List<String> lines = Files.readAllLines( file.toPath() );

		final StringBuilder buffer = new StringBuilder();
		for (Iterator<String> it = lines.iterator(); it.hasNext();) 
		{
			final String line = it.next();
			buffer.append(line);
			if ( it.hasNext() ) {
				buffer.append("\n");
			}
		}
		return buffer.toString();
	}

	private static String getText(URL url) throws IOException, URISyntaxException {

		System.out.println("Fetching text from "+url+" ...");
		URLConnection urlConnection = url.openConnection();
		urlConnection.setRequestProperty( "User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:45.0) Gecko/20100101 Firefox/45.0");
		
		final Document doc = Jsoup.parse( urlConnection.getInputStream() , "utf8" , url.toURI().toString() );
		final Element body = doc.select("body").first();
		return body.text();
	}
	
	public static void main(String[] args) throws IOException, HeadlessException, InvocationTargetException, InterruptedException, URISyntaxException 
	{
		if ( args.length == 0 ) {
			args = new String[]{"https://www.reddit.com/r/programming"};
		}
		run(args);
	}

	public static void run(String[] args) throws IOException, HeadlessException, InvocationTargetException, InterruptedException, URISyntaxException 
	{
		if ( args.length != 1 && args.length != 2 ) {
			throw new IllegalArgumentException("Need one or two arguments, got "+args.length);
		}
		
		// final String text = getText(new File("/home/tobi/mars_workspace/hashlife/LICENSE.txt" ) );
		final String text;
		if ( ! args[0].contains("://" ) ) {
			text = getText( new File( args[0] ) );
		} else {
			text = getText( new URL( args[0] ) );
		}

		final BufferedImage mask;
		if ( args.length == 2 ) {
			mask = ImageIO.read( new File("/home/tobi/tmp/heart.png" ) );
		} else {
			mask = new BufferedImage( IMAGE_SIZE.width , IMAGE_SIZE.height , BufferedImage.TYPE_INT_RGB );
			final Graphics2D g = mask.createGraphics();
			g.setColor(Color.BLACK);
			g.fillRect( 0 , 0 , IMAGE_SIZE.width , IMAGE_SIZE.height );
			g.dispose();
		}
		
		final AbstractWordFrequencyProvider provider = new StringWordFrequencyProvider( text );
		
		final BufferedImage image = new Main().renderWordCloud( provider , mask );

		SwingUtilities.invokeAndWait( () -> 
		{
			final JFrame frame = new JFrame();
			final JPanel panel = new JPanel() {

				@Override
				protected void paintComponent(Graphics g) 
				{
					g.drawImage( image , 0 , 0 , getWidth() , getHeight() , null );
				}
			};

			panel.setPreferredSize( IMAGE_SIZE );
			frame.getContentPane().setLayout( new BorderLayout() );
			frame.getContentPane().add( panel , BorderLayout.CENTER );
			frame.pack();
			frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
			frame.setVisible( true );
			frame.setLocationRelativeTo( null );
		});
	}
	
	protected static final class Pair {
		public final String word;
		public final int count;
		
		public Pair(String word, int count) {
			this.word = word;
			this.count = count;
		}
	}

	public BufferedImage renderWordCloud(IWordFrequencyProvider provider,BufferedImage mask) throws IOException 
	{
		final Map<String, Integer> countByWords = provider.getWordCounts( WORDS_TO_RENDER );
		int totalCount = provider.getTotalWordCount();

		List<Pair> pairs = countByWords.entrySet().stream().map( entry -> new Pair(entry.getKey(),entry.getValue() ) ).sorted( (p1,p2) -> Integer.compare( p2.count ,  p1.count ) ).collect( Collectors.toCollection( ArrayList::new ) );
		if ( WORDS_TO_DISCARD > 0 && pairs.size() > WORDS_TO_DISCARD ) {
			// remove top-n words
			final int ignoredCount = pairs.stream().limit( WORDS_TO_DISCARD ).peek( p -> System.out.println("Discarding "+p.word) ).mapToInt( entry -> entry.count ).sum();
			totalCount -= ignoredCount;
			pairs = pairs.subList( WORDS_TO_DISCARD ,  pairs.size() );
		}
		
		final int finalCount = totalCount;
		ArrayList<WordEntry> wordList = pairs.stream().map( pair -> new WordEntry(pair,finalCount ) ).collect( Collectors.toCollection( ArrayList::new ) );

		// normalize so that most frequent word gets 100 %
		double maxFrequency = wordList.stream().mapToDouble( entry -> entry.percentage ).max().orElse( 1 );
		final double scale = 1d/maxFrequency;
		wordList.stream().forEach( entry -> entry.percentage *=scale );

		image = new BufferedImage( IMAGE_SIZE.width , IMAGE_SIZE.height , BufferedImage.TYPE_INT_RGB );
		graphics = image.createGraphics();
		
		if ( mask != null ) 
		{
			graphics.drawImage( mask , 0 , 0 , image.getWidth() , image.getHeight() , null );
		} else {
			graphics.setColor( Color.BLACK );
			graphics.fillRect( 0 , 0 , image.getWidth() , image.getHeight() );
		}
		graphics.setColor( Color.RED );

		wordList.forEach( System.out::println );

		for (int i = 0; i < wordList.size(); i++) {
			final WordEntry entry = wordList.get(i);
			System.out.println("Placing word "+(i+1+" of "+wordList.size() ) );
			System.out.flush();
			render( entry , image.getWidth() , image.getHeight() );
		}
		return image;
	}	

	private void calculateSumImage() {

		final int w = IMAGE_SIZE.width;
		final int h = IMAGE_SIZE.height;
		int rowPtr = 0;
		for ( int y = 0 ; y < h ; y++ ) 
		{
			for ( int x = 0 ; x < w ; x++ ) 
			{
				int value =  image.getRGB( x , y )  != 0xff000000 ? 1 : 0;
				final int predX = x > 0 ? getSum(x-1,y) : 0;
				final int predY = y > 0 ? getSum(x,y-1) : 0;
				value += predX + predY - ( ( x > 0 && y > 0 ) ? getSum(x-1,y-1) : 0 );
				sumImage[ rowPtr + x ] = value;
			}
			rowPtr += w;
		}
	}

	private int getSum(int x,int y) 
	{
		return sumImage[ y*IMAGE_SIZE.width + x ];
	}

	private void render(WordEntry entry,int scrWidth,int scrHeight) 
	{
		calculateSumImage();

		// find rectangle
		float fontSize = (float) (Math.log10( 1.7 + 10*Math.pow(entry.percentage,2.5)) * MAX_FONT_SIZE);
		if ( fontSize < MIN_FONT_SIZE ) {
			fontSize = MIN_FONT_SIZE;
		}
		final Font font = graphics.getFont().deriveFont( fontSize );

		final Font oldFont = graphics.getFont();

		graphics.setFont( font );
		final FontMetrics metrics = graphics.getFontMetrics();

		// string bounds in BASELINE-RELATIVE COORDINATES
		final Rectangle2D bounds = metrics.getStringBounds( entry.word , graphics );

		final int BORDER_X = 5;
		final int BORDER_Y = 5;

		final boolean vertical = false; // rnd.nextBoolean();
		
		int width = 2*BORDER_X + (int) Math.ceil( bounds.getWidth() );
		int height = 2*BORDER_Y + (int) Math.ceil( bounds.getHeight() );		
		if ( vertical ) {
			int tmp = width;
			width = height;
			height = tmp;
		}
		
		// find a rectangle of this size that is empty
		final int X_BOUND = IMAGE_SIZE.width - width;
		final int Y_BOUND = IMAGE_SIZE.height - height ;
		int iteration = 0;
		while ( true ) 
		{
			iteration++;
			if ( (iteration%100000) == 0 ) {
				if ( iteration >= 500000 ) 
				{
					if ( fontSize <= MIN_FONT_SIZE ) {
						System.out.println("Giving up on '"+entry.word+"'");
						return;
					}
					fontSize *=0.9f;
					iteration = 0;
				}
			}
			
			final int x0 = rnd.nextInt( 0 , X_BOUND  );  // top-left X
			final int y0 = rnd.nextInt( 0 , Y_BOUND  );  // top-left Y

			final int sum1 = getSum(x0,y0);
			final int sum4 = getSum(x0+width,y0+height);
			final int sum2 = getSum(x0+width,y0);
			final int sum3 = getSum(x0,y0+height);
			
			final int sum = sum1+sum4-sum2-sum3;
			
			if ( sum == 0 ) {
				// we found a place to render our word
				
				if ( vertical ) 
				{
				    final AffineTransform old = graphics.getTransform();
				    
				    final AffineTransform rotationMatrix = new AffineTransform();
				    rotationMatrix.setToRotation(-Math.PI / 2.0, x0+width/2 , y0+height/2 );
				    graphics.setTransform(rotationMatrix);
				    graphics.drawString( entry.word , x0+BORDER_X , y0 + BORDER_Y + metrics.getAscent() );
				    graphics.setTransform( old );
				} else {
					graphics.drawString( entry.word , x0+BORDER_X , y0 + BORDER_Y + metrics.getAscent() );
				}
//				graphics.drawRect( x0 ,y0 , width , height ); 
				graphics.setFont( oldFont );
				return;
			}
		}
	}
}