/*
 * Copyright (C) 2014 mkleint
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tropyx.nb_puppet.parser;

import com.tropyx.nb_puppet.lexer.PTokenId;
import static com.tropyx.nb_puppet.parser.PuppetParser.nextSkipWhitespaceComment;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import static com.tropyx.nb_puppet.parser.PuppetParser.matches;

/**
 * Service, Resource['file'], class, file, Type[Resource[File]], Resource[File, '/tmp/foo', '/tmp/bar']
 * @author mkleint
 */
public class PTypeReference extends PElement {

//    private static String[] CORE_DATA_TYPES = new String[] {
//        "String", "Integer", "Float", "Numeric", "Boolean",
//         "Array", "Hash", "Regexp", "Undef", "Default"};
//
//    private static String[] ABSTRACT_DATA_TYPES = new String[]{"Scalar",
//        "Collection", "Variant", "Data", "Pattern", "Enum",
//        "Tuple", "Struct", "Optional", "Catalogentry", "Type",
//        "Any", "Callable"};

    private final String type;

    private final List<PElement> params = new ArrayList<>();
    private boolean representsClass;
    private boolean representsResource;
    private int endOffset;
    private final boolean dataType;


    public PTypeReference(PElement parent, int offset, String type) {
        super(TYPE_REF, parent, offset);
        endOffset = offset;
        this.dataType = Character.isUpperCase(type.charAt(0));
        this.type = upCaseType(type);
    }

    public void addParam(PElement param) {
        params.add(param);
    }

    public List<PElement> getParams() {
        return params;
    }

    public String getType() {
        return type;
    }

    public boolean isDataType() {
        return dataType;
    }

    @Override
    public String toString() {
        return super.toString() + "[" + getType()+ "]";
    }

    void parseTypes(PTypeReference ref, String name, TokenSequence<PTokenId> ts) {
        Token<PTokenId> token = ts.token();
        if (null != name) switch (name) {
            case "String":
                parseParams(token, ts, ref, new PTokenId[] {PTokenId.INT_LITERAL}, new PTokenId[] {PTokenId.INT_LITERAL});
                break;
            case "Integer":
                parseParams(token, ts, ref, new PTokenId[] {PTokenId.INT_LITERAL, PTokenId.DEFAULT}, new PTokenId[] {PTokenId.INT_LITERAL, PTokenId.DEFAULT});
                break;
            case "Float":
                parseParams(token, ts, ref, new PTokenId[] {PTokenId.FLOAT_LITERAL, PTokenId.DEFAULT}, new PTokenId[] {PTokenId.FLOAT_LITERAL, PTokenId.DEFAULT});
                break;
            case "Numeric":
                parseParams(token, ts, ref, new PTokenId[] {PTokenId.INT_LITERAL, PTokenId.FLOAT_LITERAL, PTokenId.DEFAULT}, new PTokenId[] {PTokenId.INT_LITERAL, PTokenId.FLOAT_LITERAL, PTokenId.DEFAULT});
                break;
            case "Boolean": case "Undef": case "Default" :
            case "Scalar": case "Data": case "Collection":
            case "Catalogentry": case "Any":
                parseParams(token, ts, ref);
                break;
            case "Array" :
                parseParams(token, ts, ref, new PTokenId[] {PTokenId.IDENTIFIER}, new PTokenId[] {PTokenId.INT_LITERAL, PTokenId.DEFAULT}, new PTokenId[] {PTokenId.INT_LITERAL, PTokenId.DEFAULT});
                break;
            case "Hash" :
                parseParams(token, ts, ref, new PTokenId[] {PTokenId.IDENTIFIER}, new PTokenId[] {PTokenId.IDENTIFIER},
                        new PTokenId[] {PTokenId.INT_LITERAL, PTokenId.DEFAULT}, new PTokenId[] {PTokenId.INT_LITERAL, PTokenId.DEFAULT});
                break;
            case "Regexp" :
                parseParams(token, ts, ref, new PTokenId[] {PTokenId.REGEXP_LITERAL});
                break;
            case "Optional" :
                //TODO the param is mandatory
                parseParams(token, ts, ref, new PTokenId[] {PTokenId.IDENTIFIER, PTokenId.STRING_LITERAL});
                break;
            case "NotUndef" :
                parseParams(token, ts, ref, new PTokenId[] {PTokenId.IDENTIFIER, PTokenId.STRING_LITERAL});
                break;
            case "Variant" :
                parseParams(token, ts, ref, true, new PTokenId[] {PTokenId.IDENTIFIER });
                break;
            case "Pattern" :
                parseParams(token, ts, ref, true, new PTokenId[] {PTokenId.REGEXP_LITERAL });
                break;
            case "Enum" :
                parseParams(token, ts, ref, true, new PTokenId[] {PTokenId.STRING_LITERAL });
                break;
            case "Tuple" :
                parseParams(token, ts, ref, true, new PTokenId[] {PTokenId.IDENTIFIER }); //TODO, new PTokenId[] {PTokenId.INT_LITERAL }, new PTokenId[] {PTokenId.INT_LITERAL });
                break;
            case "Type" :
                parseParams(token, ts, ref, new PTokenId[] {PTokenId.IDENTIFIER });
                break;
            case "Class":
                ref.setRepresentsClass(true);
                parseParams(token, ts, ref, new PTokenId[] {PTokenId.STRING_LITERAL });
                break;
            case "Resource":
                ref.setRepresentsResource(true);
                parseParams(token, ts, ref, true, new PTokenId[] {PTokenId.STRING_LITERAL, PTokenId.IDENTIFIER }, new PTokenId[] {PTokenId.STRING_LITERAL, PTokenId.VARIABLE});
                break;
            case "Struct":
                //TODO complex.https://docs.puppetlabs.com/puppet/4.2/reference/lang_data_abstract.html#struct
                break;
            default:
                //resource names..
                ref.setRepresentsResource(true);
                parseParams(token, ts, ref, true, new PTokenId[] {PTokenId.STRING_LITERAL, PTokenId.VARIABLE });
        }
    }

    public void parseParams(Token<PTokenId> token, TokenSequence<PTokenId> ts, PTypeReference ref,
                     PTokenId[]... params) {
        parseParams(token, ts, ref, false, params);
    }


    public void parseParams(Token<PTokenId> token, TokenSequence<PTokenId> ts, PTypeReference ref,
                    boolean recursive, PTokenId[]... params) {
        if (matches(token, PTokenId.LBRACKET)) {
            //TODO if params.length == 0 (no params but we get [, we have an error state
            token = nextSkipWhitespaceComment(ts);
            int index = 0;
            boolean atLeastOne = false;
            while (params.length > index && matches(token, params[index])) {
                if (matches(token, PTokenId.IDENTIFIER) && !isUppercase(token)) {
                    //TODO forward to correct? RBRACKET
                    new PError(ref, ts.offset());
                    break;
                }
                ref.addParam(createParam(ref, ts));
                atLeastOne = true;
                token = nextSkipWhitespaceComment(ts);
                if (matches(token, PTokenId.COMMA)) {
                    token = nextSkipWhitespaceComment(ts);
                    if (recursive) {
                        if (params.length != index + 1) {
                            index++;
                        }
                    } else {
                        index++;
                    }
                }
                
            }
            if (!matches(token, PTokenId.RBRACKET)) {
                new PError(ref, ts.offset());
                //TODO forward to correct? RBRACKET
            } else {
                if (!atLeastOne) {
                    //String[]
                    new PError(ref, ts.offset());
                }
            }
            ref.setEndOffset(ts.offset() + ts.token().length());
        } else {
            //we are without [, need to backoff
            PuppetParser.prevBackoffWhitespaceComment(ts);
            ref.setEndOffset(ts.offset() + ts.token().length());
        }
    }

    private PElement createParam(PTypeReference ref, TokenSequence<PTokenId> ts) {
        Token<PTokenId> token = ts.token();
        if (token.id() == PTokenId.INT_LITERAL) {
            return new PNumber(ref, ts.offset(), ts.offset() + token.text().length(),
                        Integer.parseInt(token.text().toString()));
        } else if (token.id() == PTokenId.IDENTIFIER && isUppercase(token)) {
            //TODO  things with brackets Variant[String, SSS]
            return new PTypeReference(ref, ts.offset(), token.text().toString());
        } else if (token.id() == PTokenId.FLOAT_LITERAL) {
            return new PFloat(ref, ts.offset(), ts.offset() + token.text().length(),
                        Float.parseFloat(token.text().toString()));
        } else if (token.id() == PTokenId.DEFAULT) {
            return new PIdentifier(ref, ts.offset(), token.text().toString());
        } else if (token.id() == PTokenId.REGEXP_LITERAL) {
            return new PRegexp(ref, ts.offset(), token.text().toString());
        } else if (token.id() == PTokenId.STRING_LITERAL) {
            if (ref.representsClass) {
                PClassRef cr = new PClassRef(ref, ts.offset());
                cr.setName(new PIdentifier(cr, ts.offset() + 1, token.text().toString().substring(1, token.text().toString().length() - 1)));
                return cr;
            }
            if (ref.representsResource) {
                if ("Resource".equals(ref.getType()) && ref.getParams().isEmpty()) {
                    //first string is actually type
                    String tp = token.text().toString().substring(1, token.text().toString().length() - 1);

                    //TODO  things with brackets Variant[String, SSS]??
                    return new PTypeReference(ref, ts.offset() + 1, upCaseType(tp));
                }
                PResourceRef rr = new PResourceRef(ref, ts.offset());
                rr.setName(new PIdentifier(rr, ts.offset() + 1, token.text().toString().substring(1, token.text().toString().length() - 1)));
                //STRING to wrap PVariables in..
                new PString(rr, ts.offset(), token.text().toString());
                return rr;
            }
            return new PString(ref, ts.offset(), token.text().toString());
        } else if (token.id() == PTokenId.VARIABLE) {
            if (ref.representsClass) {
                PClassRef cr = new PClassRef(ref, ts.offset());
                cr.setName(new PIdentifier(cr, ts.offset() + 1, token.text().toString().substring(1, token.text().toString().length() - 1)));
                return cr;
            }
            if (ref.representsResource) {
                PResourceRef rr = new PResourceRef(ref, ts.offset());
                rr.setName(new PIdentifier(rr, ts.offset(), token.text().toString().substring(1, token.text().toString().length())));
                //embed PVariable
                new PVariable(rr, ts.offset(), token.text().toString());
                return rr;
            }
            return new PVariable(ref, ts.offset(), token.text().toString());
        }
        throw new IllegalStateException("token:" + token.text());
    }

    private String upCaseType(String tp) {
        StringBuilder sb = new StringBuilder();
        for (String s : tp.split("::")) {
            if (sb.length() > 0) sb.append("::");
            sb.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1));
        }
        return sb.toString();
    }

    public static boolean isUppercase(Token<PTokenId> token) {
        return Character.isUpperCase(token.text().toString().charAt(0));
    }

    void setRepresentsClass(boolean b) {
        this.representsClass = b;
    }

    void setRepresentsResource(boolean b) {
        this.representsResource = b;
    }

    boolean isRepresentsClass() {
        return representsClass;
    }

    boolean isRepresentsResource() {
        return representsResource;
    }

    private void setEndOffset(int i) {
        this.endOffset = i;
    }

    @Override
    public int getEndOffset() {
        return endOffset;
    }


   
}
