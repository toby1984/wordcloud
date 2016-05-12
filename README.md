Extracts text from a given URL, ranks all words by their frequency and then renders the 100 most-common words in a random cloud, scaling the font size by the word frequency.

![Screenshot](https://github.com/toby1984/wordcloud/blob/master/screenshot.png)


# Requirements

- Maven 3.x
- JDK 8

# Building

```mvn clean package```

# Running

```java -jar target/wordcloud.jar [URL]```

If no URL is given, then http://www.reddit.com/r/programming is used.
