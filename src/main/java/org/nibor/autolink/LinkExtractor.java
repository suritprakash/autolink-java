package org.nibor.autolink;

import org.nibor.autolink.internal.EmailScanner;
import org.nibor.autolink.internal.Scanner;
import org.nibor.autolink.internal.UrlScanner;

import java.util.*;

/**
 * Extracts links from input.
 * <p/>
 * Create and configure an extractor using {@link #builder()}, then call {@link #extractLinks}.
 */
public class LinkExtractor {

    private static Scanner URL_SCANNER = new UrlScanner();
    private static Scanner EMAIL_SCANNER = new EmailScanner();

    private final Set<LinkType> linkTypes;

    private LinkExtractor(Set<LinkType> linkTypes) {
        this.linkTypes = linkTypes;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Extract the links from the input text. Can be called multiple times with different inputs (thread-safe).
     *
     * @param input the input text, must not be {@code null}
     * @return a lazy iterable for the links in order that they appear in the input, never {@code null}
     */
    public Iterable<LinkSpan> extractLinks(final CharSequence input) {
        return new Iterable<LinkSpan>() {
            @Override
            public Iterator<LinkSpan> iterator() {
                return new LinkIterator(input);
            }
        };
    }

    private Scanner trigger(char c) {
        switch (c) {
            case ':':
                if (linkTypes.contains(LinkType.URL)) {
                    return URL_SCANNER;
                }
                break;
            case '@':
                if (linkTypes.contains(LinkType.EMAIL)) {
                    return EMAIL_SCANNER;
                }
        }
        return null;
    }

    /**
     * Builder for configuring link extractor.
     */
    public static class Builder {

        private Set<LinkType> linkTypes = EnumSet.allOf(LinkType.class);

        private Builder() {
        }

        /**
         * @param linkTypes the link types that should be extracted (by default, all types are extracted)
         * @return this builder
         */
        public Builder linkTypes(Set<LinkType> linkTypes) {
            this.linkTypes = new HashSet<>(Objects.requireNonNull(linkTypes, "linkTypes must not be null"));
            return this;
        }

        /**
         * @return the configured link extractor
         */
        public LinkExtractor build() {
            return new LinkExtractor(linkTypes);
        }
    }

    private class LinkIterator implements Iterator<LinkSpan> {

        private final CharSequence input;

        private LinkSpan next = null;
        private int index = 0;
        private int rewindIndex = 0;

        public LinkIterator(CharSequence input) {
            this.input = input;
        }

        @Override
        public boolean hasNext() {
            setNext();
            return next != null;
        }

        @Override
        public LinkSpan next() {
            if (hasNext()) {
                LinkSpan link = next;
                next = null;
                return link;
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        private void setNext() {
            if (next != null) {
                return;
            }

            int length = input.length();
            while (index < length) {
                Scanner scanner = trigger(input.charAt(index));
                if (scanner != null) {
                    LinkSpan link = scanner.scan(input, index, rewindIndex);
                    if (link != null) {
                        next = link;
                        index = link.getEndIndex();
                        rewindIndex = index;
                        break;
                    } else {
                        index++;
                    }
                } else {
                    index++;
                }
            }
        }
    }
}
