
package brad.grier.alhena;


import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

public class GemtextTokenMaker extends AbstractTokenMaker {

    private static final int STATE_NORMAL = 0;
    private static final int STATE_PREFORMATTED = 1;

    @Override
    public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
        resetTokenList();
        int offset = startOffset;
        int state = initialTokenType;

        int lineStart = text.offset;
        int lineEnd = text.offset + text.count;

        String line = new String(text.array, lineStart, text.count);
        boolean isPreDelimiter = line.startsWith("```");

        if (state == STATE_PREFORMATTED) {
            if (isPreDelimiter) {
                addToken(text.array, lineStart, lineEnd - 1, TokenTypes.COMMENT_MULTILINE, offset);
                state = STATE_NORMAL;
            } else {
                addToken(text.array, lineStart, lineEnd - 1, TokenTypes.LITERAL_STRING_DOUBLE_QUOTE, offset);
            }
            addNullToken();
            return firstToken;
        }

        if (isPreDelimiter) {
            addToken(text.array, lineStart, lineEnd - 1, TokenTypes.COMMENT_MULTILINE, offset);
            addNullToken();
            return firstToken;
        }

        int i = lineStart;
        int firstChar = i < lineEnd ? text.array[i] : -1;

        if (firstChar == '#') {
            addToken(text.array, i, lineEnd - 1, TokenTypes.RESERVED_WORD, offset);
        } else if (firstChar == '*' && (i + 1 < lineEnd && text.array[i + 1] == ' ')) {
            addToken(text.array, i, lineEnd - 1, TokenTypes.OPERATOR, offset);
        } else if (firstChar == '>') {
            addToken(text.array, i, lineEnd - 1, TokenTypes.COMMENT_EOL, offset);
        } else if (firstChar == '=' && (i + 1 < lineEnd && text.array[i + 1] == '>')) {
            addToken(text.array, i, lineEnd - 1, TokenTypes.FUNCTION, offset);
        } else {
            // tokenize plain text line (allow wrapping by splitting into words)
            int tokenStart = i;
            while (i < lineEnd) {
                char c = text.array[i];

                if (Character.isWhitespace(c)) {
                    if (tokenStart < i) {
                        addToken(text.array, tokenStart, i - 1, TokenTypes.IDENTIFIER, offset + (tokenStart - lineStart));
                    }
                    addToken(text.array, i, i, TokenTypes.WHITESPACE, offset + (i - lineStart));
                    i++;
                    tokenStart = i;
                } else {
                    i++;
                }
            }

            if (tokenStart < lineEnd) {
                addToken(text.array, tokenStart, lineEnd - 1, TokenTypes.IDENTIFIER, offset + (tokenStart - lineStart));
            }
        }

        addNullToken();
        return firstToken;
    }

    @Override
    public int getLastTokenTypeOnLine(Segment text, int initialTokenType) {
        String line = new String(text.array, text.offset, text.count);
        if (line.startsWith("```")) {
            return initialTokenType == STATE_PREFORMATTED ? STATE_NORMAL : STATE_PREFORMATTED;
        }
        return initialTokenType;
    }

    @Override
    public boolean getCurlyBracesDenoteCodeBlocks(int languageIndex) {
        return false;
    }

    @Override
    public boolean isIdentifierChar(int index, char c) {
        return Character.isLetterOrDigit(c) || c == '-' || c == '_';
    }

    @Override
    public TokenMap getWordsToHighlight() {
        return new TokenMap(); // not used for Gemtext
    }
}