package symphony.apt.tests;

import symphony.annotations.java.GQLDescription;
import symphony.apt.annotation.ObjectSchema;
import symphony.apt.annotation.UnionSchema;

@UnionSchema
@GQLDescription("SearchResult")
public sealed interface SearchResult permits Book, Author {

    SearchResult book = new Book("book");
    SearchResult author = new Author("author");
}

@ObjectSchema
record Book(String title) implements SearchResult {
}

@ObjectSchema
record Author(String name) implements SearchResult {
}