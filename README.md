Extracts text from a given URL, ranks all words by their frequency and then renders the 100 most-common words in a random cloud, scaling the font size by the word frequency.

The code uses a Summed Area Table (https://en.wikipedia.org/wiki/Summed_area_table) to speed-up searching for a place where to put a word ; this idea was shamelessly stolen from http://peekaboo-vision.blogspot.de/2012/11/a-wordcloud-in-python.html

![Screenshot](https://github.com/toby1984/wordcloud/blob/master/screenshot.png)


# Requirements

- Maven 3.x
- JDK 8

# Building

```mvn clean package```

# Running

```java -jar target/wordcloud.jar [URL]```

If no URL is given, then http://www.reddit.com/r/programming is used.
