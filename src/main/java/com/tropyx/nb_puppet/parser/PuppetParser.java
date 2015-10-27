/*
 * Copyright (C) 2015 mkleint
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.event.ChangeListener;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;

class PuppetParser extends Parser {

    private final AtomicBoolean cancelled = new AtomicBoolean();

    private PuppetParserResult result;

    public PuppetParser() {
    }

    @Override
    public void parse(Snapshot snapshot, Task task, SourceModificationEvent event) throws ParseException {
//        System.out.println("text" + snapshot.getText().toString());
        result = doParse(snapshot, task);
        System.out.println("AST:" + result.getRootNode().toStringRecursive());
    }

    @Override
    public Result getResult(Task task) throws ParseException {
        return result;
    }

    @Override
    public void addChangeListener(ChangeListener changeListener) {
    }

    @Override
    public void removeChangeListener(ChangeListener changeListener) {
    }

    @Override
    public void cancel(CancelReason reason, SourceModificationEvent event) {
        cancelled.set(true);
    }

    @Override
    public void cancel() {
        cancelled.set(true);
    }

    private PuppetParserResult doParse(Snapshot snapshot, Task task) {
        TokenSequence<PTokenId> ts = (TokenSequence<PTokenId>) snapshot.getTokenHierarchy().tokenSequence();
        ts.moveStart();
        final PElement root = new PElement(PElement.ROOT, null, 0 );
        Token<PTokenId> token = nextSkipWhitespaceComment(ts);
        while (token != null && ts.isValid()) {
            if (token.id() == PTokenId.CLASS) {
                parseClass(root, ts);
            } 
            else if (token.id() == PTokenId.NODE) {
                parseNode(root, ts);
            } else if (token.id() == PTokenId.DEFINE) {
                parseDefine(root, ts);
            }
            token = nextSkipWhitespaceComment(ts);
            //TODO what about site.pp (without define/class/node)?
        }

        return new PuppetParserResult(snapshot, root);
    }

    static Token<PTokenId> skipWhitespaceComment(TokenSequence<PTokenId> ts) {
        while (ts.token() != null && (ts.token().id() == PTokenId.WHITESPACE || ts.token().id() == PTokenId.COMMENT || ts.token().id() == PTokenId.LINE_COMMENT))
        {
            if (!ts.moveNext()) {
                return null;
            }
        }
        return ts.token();
    }

    static Token<PTokenId> nextSkipWhitespaceComment(TokenSequence<PTokenId> ts) {
        if (!ts.moveNext()) {
            return null;
        }
        return skipWhitespaceComment(ts);
    }
    static Token<PTokenId> backoffWhitespaceComment(TokenSequence<PTokenId> ts) {
        while (ts.token() != null && (ts.token().id() == PTokenId.WHITESPACE || ts.token().id() == PTokenId.COMMENT || ts.token().id() == PTokenId.LINE_COMMENT))
        {
            if (!ts.movePrevious()) {
                return null;
            }
        }
        return ts.token();
    }

    static Token<PTokenId> prevBackoffWhitespaceComment(TokenSequence<PTokenId> ts) {
        if (!ts.movePrevious()) {
            return null;
        }
        return backoffWhitespaceComment(ts);
    }

    private String collectText(TokenSequence<PTokenId> ts, PTokenId... stopTokens) {
        StringBuilder name = new StringBuilder();
        Token<PTokenId> token = ts.token();
        List<PTokenId> stops = Arrays.asList(stopTokens);
        while (token != null && !stops.contains(token.id())) {
            name.append(token.text().toString());
            ts.moveNext();
            token = ts.token();
        }
        if (token == null) {
            return null;
        }
        return name.toString();
    }

    private PBlob fastForward(@NullAllowed PElement parent, TokenSequence<PTokenId> ts, boolean allowResources, PTokenId... stopTokens) {
        PBlob blob = new PBlob(parent, ts.offset());
        return fastForwardImpl(blob, ts, allowResources, stopTokens);
    }

    private PBlob fastForwardImpl(@NonNull PBlob blob, TokenSequence<PTokenId> ts, boolean allowResources, PTokenId... stopTokens) {
        Token<PTokenId> token = ts.token();
        List<PTokenId> stops = Arrays.asList(stopTokens);
        int braceCount = 0;
        int bracketCount = 0;
        int parenCount = 0;
        boolean ignore = false;

        while (token != null && (ignore || !stops.contains(token.id()))) {
            if (null != token.id()) switch (token.id()) {
                case LBRACE:
                    braceCount++;
                    break;
                case RBRACE:
                    braceCount--;
                    break;
                case LBRACKET:
                    bracketCount++;
                    break;
                case RBRACKET:
                    bracketCount--;
                    break;
                case LPAREN:
                    parenCount++;
                    break;
                case RPAREN:
                    parenCount--;
                    break;
                case STRING_LITERAL:
                    String val = token.text().toString();
                    int off = ts.offset();
                    new PString(blob, off, val);
                    break;
                case VARIABLE:
                    val = token.text().toString();
                    off = ts.offset();
                    token = nextSkipWhitespaceComment(ts);
                    if (token != null && token.id() == PTokenId.EQUALS) {
                        //variable definition;
                        new PVariableDefinition(blob, off, val);
                    } else if (token != null) {
                        //variable usage
                        new PVariable(blob, off, val);
                        continue;
                    }
                    break;
                case INCLUDE:
                case REQUIRE:
                case CONTAIN:
                    //CONTAIN also should have Class['ref'], arrays + comma separated lists
                    //TODO class + resource type values??
                    // docs: You must use the class’s full name; relative names are not allowed. In addition to names in string form, you may also directly use Class and Resource Type values that are produced by the future parser’s resource and relationship expressions.
                    int offs = ts.offset();
                    String f = token.text().toString();
                    token = nextSkipWhitespaceComment(ts);
                    if (token == null) {
                        break;
                    }
                    //TODO apparently can also be $variable??
                    if (token.id() == PTokenId.IDENTIFIER) {
                        parseReqList(new PFunction(blob, offs, f), ts);
                        token = ts.token();
                        continue;
                    } else if (token.id() == PTokenId.LBRACKET) {
                        token = nextSkipWhitespaceComment(ts);
                        if (token == null || token.id() != PTokenId.IDENTIFIER) {
                            break;
                        }
                        parseReqList(new PFunction(blob, offs, f), ts);
                        token = ts.token();
                        if (token != null && token.id() == PTokenId.RBRACKET) {
                            break;
                        } else {//error? it's not IDENT , or ]

                        }
                    }
                    break;
                case IDENTIFIER:
                case CLASS:
                    val = token.text().toString();
                    off = ts.offset();
                    PTypeReference ref = null;
                    if (token.id() == PTokenId.IDENTIFIER) {
                        //check unknown functions
                        token = nextSkipWhitespaceComment(ts);
                        if (token.id() == PTokenId.LPAREN) {
                            ts.moveNext();
                            parseFunction(new PFunction(blob, off, val), ts);
                            break;
                        } else if (Character.isUpperCase(val.charAt(0))) {
                            //TODO how is array access handled? eg. aa[1]? or will we always get just $aa[1]?
                            ref = new PTypeReference(null, off, val);
                            parseTypeRef(ref, val, ts);
                            token = ts.token();
                        } else {
                            token = prevBackoffWhitespaceComment(ts);
                        }
                    }
                    if (allowResources && bracketCount == 0 && parenCount == 0) {
                        boolean isClass = token.id() == PTokenId.CLASS;
                        off = ts.offset();
                        token = nextSkipWhitespaceComment(ts);
                        if (token != null && token.id() == PTokenId.LBRACE) {
                            ref = ref != null ? ref : new PTypeReference(null, off, val);
                            if (isClass) {
                                ref.setRepresentsClass(true);
                            }
                            parseResource(blob, ref, ts, off);
                        } else if (isClass && token != null && token.id() == PTokenId.IDENTIFIER) {
                            String name = token.text().toString();
                            nextSkipWhitespaceComment(ts);
                            final PClass pClass = new PClass(blob, off);
                            parseClassInternal(pClass, new PIdentifier(pClass, ts.offset(), name), ts);
                        } else {
                            if (ref != null) {
                                ref.setParent(blob);
                            }
                            continue;
                        }
                    } else {
                        if (ref != null) {
                            ref.setParent(blob);
                            break;
                        }
                    }
                    break;
                case CASE:
                    parseCase(blob, ts);
                    break;
                case IF:
                    parseIf(blob, ts, true);
                    break;
                case UNLESS:
                    parseIf(blob, ts, false);
                    break;
                case DOT:
                    ts.moveNext();
                    token = ts.token();
                    if (token != null
                        && (token.id() == PTokenId.IDENTIFIER || PTokenId.Category.FUNCTION.equals(token.id().primaryCategory()))) {
                        off = ts.offset();
                        String func = token.text().toString();
                        token = nextSkipWhitespaceComment(ts);
                        PFunction ff = new PFunction(blob, off, func);
                        if (token.id() == PTokenId.LPAREN) {
                            ts.moveNext();
                            parseFunction(ff, ts);
                        } else {
                            prevBackoffWhitespaceComment(ts); //backoff for non () functions
                        }
                    }
                    break;
                default:
                    if (PTokenId.Category.FUNCTION.equals(token.id().primaryCategory())) {
                        off = ts.offset();
                        String func = token.text().toString();
                        token = nextSkipWhitespaceComment(ts);
                        if (token.id() == PTokenId.LPAREN) {
                            ts.moveNext();
                            parseFunction(new PFunction(blob, off, func), ts);
                        } else {
                            prevBackoffWhitespaceComment(ts); //backoff for non () functions, how to figure where they stop?
                        }
                    }
            }

            token = nextSkipWhitespaceComment(ts);
            ignore = bracketCount > 0 || braceCount > 0 || parenCount > 0;
        }
        blob.setEndOffset(ts.offset() + (token != null ? token.length() : 0));
        return blob;
    }

    //https://docs.puppetlabs.com/puppet/latest/reference/lang_defined_types.html
    private void parseDefine(PElement root, TokenSequence<PTokenId> ts) {
        PDefine pc = new PDefine(root, ts.offset());
        Token<PTokenId> token;
        if (null == nextSkipWhitespaceComment(ts)) {
            return;
        }
        String name = collectText(ts, PTokenId.WHITESPACE, PTokenId.LBRACE, PTokenId.LPAREN);
        if (name != null) {
            pc.setName(name);
            token = skipWhitespaceComment(ts);
            if (token != null && token.id() == PTokenId.LPAREN) {
                //params
                parseParams(pc, ts);
                token = nextSkipWhitespaceComment(ts);
            }
            if (token != null && token.id() == PTokenId.LBRACE) {
                //we are done for define
                //internals or skip to RBRACE
                ts.moveNext();
                fastForward(pc, ts, true, PTokenId.RBRACE);
            }
        }
    }

    //http://docs.puppetlabs.com/puppet/4.2/reference/lang_node_definitions.html
    private void parseNode(PElement root, TokenSequence<PTokenId> ts) {
        PNode pc = new PNode(root, ts.offset());
        if (null == nextSkipWhitespaceComment(ts)) {
            return;
        }
        List<String> names = new ArrayList<>();
        Token<PTokenId> token = ts.token();
        while (token != null && PTokenId.LBRACE != token.id()) {
            if (PTokenId.COMMA != token.id()) {
                String name = token.text().toString();
                if (name != null) {
                    names.add(name);
                }
            }
            token = nextSkipWhitespaceComment(ts);
        }
        pc.setNames(names.toArray(new String[0]));
        if (token != null && token.id() == PTokenId.LBRACE) {
            //we are done for node
            //internals or skip to RBRACE
            ts.moveNext();
            fastForward(pc, ts, true, PTokenId.RBRACE);
        }
    }

    //http://docs.puppetlabs.com/puppet/4.2/reference/lang_classes.html
    private void parseClass(PElement root, TokenSequence<PTokenId> ts) {
        int offset = ts.offset();
        Token<PTokenId> token = nextSkipWhitespaceComment(ts);
        if (null == token) {
            return;
        }
        if (token.id() == PTokenId.IDENTIFIER) {
            PClass pc = new PClass(root, offset);
            PIdentifier name = new PIdentifier(pc, ts.offset(), token.text().toString());
            parseClassInternal(pc, name, ts);
        }
    }
    private void parseClassInternal(PClass pc, PIdentifier name, TokenSequence<PTokenId> ts) {
        Token<PTokenId> token;
        pc.setName(name);
        token = nextSkipWhitespaceComment(ts);
        if (token != null && token.id() == PTokenId.LPAREN) {
            //params
            parseParams(pc, ts);
            token = nextSkipWhitespaceComment(ts);
        }
        if (token != null && token.id() == PTokenId.INHERITS) {
            //inherits
            token = nextSkipWhitespaceComment(ts);
            int off = ts.offset();
            if (token.id() == PTokenId.IDENTIFIER) {
                PClassRef ref = new PClassRef(pc, off);
                ref.setName(new PIdentifier(ref, off, token.text().toString()));
                pc.setInherits(ref);
                token = nextSkipWhitespaceComment(ts);
            } else {
                token = null;
            }
        }
        if (token != null && token.id() == PTokenId.LBRACE) {
            //we are done for class
            //internals or skip to RBRACE
            ts.moveNext();
            fastForward(pc, ts, true, PTokenId.RBRACE);
        }
    }

    private void parseParams(PParamContainer pc, TokenSequence<PTokenId> ts) {
        Token<PTokenId> token = nextSkipWhitespaceComment(ts);
        String type = null;
        int offset = 0;
        PVariableDefinition var = null;
        PElement def = null;
        List<PClassParam> params = new ArrayList<>();
        while (token != null && token.id() != PTokenId.RPAREN) {
            if (type == null && token.id() == PTokenId.IDENTIFIER) {
                type = token.text().toString();
                offset = ts.offset();
            }
            if (var == null && token.id() == PTokenId.VARIABLE) {
                var = new PVariableDefinition(null, ts.offset(), token.text().toString());
                type = type != null ? type : "Any";
                offset = offset != 0 ? offset : ts.offset();
            }
            if (token.id() == PTokenId.EQUALS) {
                def = fastForward(null, ts, false, PTokenId.RPAREN, PTokenId.COMMA);
                token = ts.token();
                if (token.id() == PTokenId.RPAREN) {
                    break;
                }
            }
            if (token.id() == PTokenId.COMMA) {
                assert var != null && type != null : "var:" + var + " type:" + type + " for pc:" + pc.toString();
                PClassParam param = new PClassParam((PElement)pc, offset, var);
                param.setTypeType(type);
                if (def != null) {
                    def.setParent(param);
                    param.setDefaultValue(def);
                }
                params.add(param);
                type = null;
                var = null;
                def = null;
                offset = 0;
            }
            //TODO default values
            token = nextSkipWhitespaceComment(ts);
        }
        if (var != null) {
            assert type != null;
            PClassParam param = new PClassParam((PElement)pc, offset, var);
            param.setTypeType(type);
            if (def != null) {
                def.setParent(param);
                param.setDefaultValue(def);
            }
            params.add(param);
        }
        pc.setParams(params.toArray(new PClassParam[0]));
    }

    private @CheckForNull PResource parseResource(PElement pc, PTypeReference type, TokenSequence<PTokenId> ts, int resOff) {
        if ("Class".equals(type.getType())) {
            type.setRepresentsClass(true);
        } else {
            type.setRepresentsResource(true);
        }
        if (type.isDataType()) {
            // File { } -> no titles
            PResource resource = new PResource(pc, resOff, type);
            parseResourceAttrs(resource, ts);
            return resource;
        }
        List<PElement> titles = new ArrayList<>();
        Token<PTokenId> token = nextSkipWhitespaceComment(ts);
        boolean inBracket = false;
        while (token != null) {
            if (token.id() == PTokenId.STRING_LITERAL) {
                if (type.isRepresentsClass()) {
                    PClassRef cr = new PClassRef(null, ts.offset());
                    final PIdentifier pIdentifier = new PIdentifier(cr, ts.offset() + 1, token.text().toString().substring(1, token.text().toString().length() - 1));
                    cr.setName(pIdentifier);
                    titles.add(cr);
                    //STRING to wrap PVariables in..
                    new PString(pIdentifier, ts.offset(), token.text().toString());
                } else if (type.isRepresentsResource()) {

                    PResourceRef rr = new PResourceRef(null, ts.offset());
                    final PIdentifier pIdentifier = new PIdentifier(rr, ts.offset() + 1, token.text().toString().substring(1, token.text().toString().length() - 1));
                    rr.setName(pIdentifier);
                    //STRING to wrap PVariables in..
                    new PString(pIdentifier, ts.offset(), token.text().toString());
                    titles.add(rr);
                } else {
                    throw new IllegalStateException();
                }
            } else if (token.id() == PTokenId.VARIABLE) {
                if (type.isRepresentsClass()) {
                    PClassRef cr = new PClassRef(null, ts.offset());
                    cr.setName(new PIdentifier(cr, ts.offset() + 1, token.text().toString().substring(1, token.text().toString().length() - 1)));
                    titles.add(cr);
                }
                else if (type.isRepresentsResource()) {
                    PResourceRef rr = new PResourceRef(null, ts.offset());
                    final PIdentifier pIdentifier = new PIdentifier(rr, ts.offset(), token.text().toString().substring(1, token.text().toString().length()));
                    rr.setName(pIdentifier);
                    //embed PVariable
                    new PVariable(pIdentifier, ts.offset(), token.text().toString());
                    titles.add(rr);
                } else {
                    throw new IllegalStateException();
                }
            } else if (token.id() == PTokenId.LBRACKET) {
                inBracket = true;
            } else if (token.id() == PTokenId.RBRACKET) {
                inBracket = false;
            } else if (token.id() == PTokenId.COMMA) {
                if (inBracket) {
                    token = nextSkipWhitespaceComment(ts);
                    continue;
                } else {
                    new PError(pc, ts.offset());
                    return null;
                }
            } else {
                PTokenId[] stops;
                if (inBracket) {
                    stops = new PTokenId[] { PTokenId.COLON, PTokenId.COMMA, PTokenId.RBRACKET};
                } else {
                    stops = new PTokenId[] { PTokenId.COLON, PTokenId.COMMA};
                }
                titles.add(fastForward(null, ts, false, stops));
                token = ts.token();
                if (matches(token, PTokenId.COMMA)) {

                } else if (inBracket && matches(token, PTokenId.RBRACKET)) {
                    inBracket = false;
                }
                prevBackoffWhitespaceComment(ts);
            }
            token = nextSkipWhitespaceComment(ts);
            if (token != null && token.id() == PTokenId.COLON) {
                if (inBracket) {
                    //Error?
                    continue;
                }
                PResource resource = new PResource(pc, resOff, type);
                type.setParent(resource);
                for (PElement title : titles) {
                    title.setParent(resource);
                    resource.addTitle(title);
                }
                parseResourceAttrs(resource, ts);
                return resource;
            }
            if (token != null && token.id() == PTokenId.RBRACE) {
                new PError(pc, ts.offset());
                return null;
            }
        }
        return null;
    }

    private void parseResourceAttrs(PResource resource, TokenSequence<PTokenId> ts) {
        Token<PTokenId> token = nextSkipWhitespaceComment(ts);
        String attr = null;
        PElement val = null;
        int off = 0;
        while (token != null && token.id() != PTokenId.RBRACE) {
            if (attr == null && (token.id() == PTokenId.IDENTIFIER || token.id() == PTokenId.UNLESS)) {
                off = ts.offset();
                attr = token.text().toString();
            }
            if (token.id() == PTokenId.PARAM_ASSIGN) {
                nextSkipWhitespaceComment(ts);
                val = fastForward(null, ts, false, PTokenId.COMMA, PTokenId.RBRACE);
                token = ts.token();
                continue;
            }
            if (token.id() == PTokenId.COMMA) {
//                assert attr != null && val != null : "attr:" + attr + " val:" + val + " in resource:" + resource.toString();
                PResourceAttribute param = new PResourceAttribute(resource, off, attr);
                val.setParent(param);
                param.setValue(val);
                resource.addAttribute(param);
                attr = null;
                val = null;
                off = 0;
            }
            token = nextSkipWhitespaceComment(ts);
        }
        if (attr != null) {
//            assert val != null;
            PResourceAttribute param = new PResourceAttribute(resource, off, attr);
            param.setValue(val);
            if (val != null) {
                val.setParent(param);
            }
            resource.addAttribute(param);
        }
    }

    private void parseCase(PElement parent, TokenSequence<PTokenId> ts) {
        PCase pcase = new PCase(parent, ts.offset());
        nextSkipWhitespaceComment(ts);
        PBlob caseExpr = fastForward(pcase, ts, false, PTokenId.LBRACE);
        pcase.setControl(caseExpr);
        Token<PTokenId> token = ts.token();
        nextSkipWhitespaceComment(ts);
        while (token.id() != PTokenId.RBRACE) {
            PBlob cas = fastForward(pcase, ts, false, PTokenId.COLON);
            nextSkipWhitespaceComment(ts);
            token = ts.token();
            PBlob caseBody;
            if (token.id() == PTokenId.LBRACE) {
                nextSkipWhitespaceComment(ts);
                caseBody = fastForward(pcase, ts, true, PTokenId.RBRACE);
                pcase.addCase(cas, caseBody);
            } else {
                //huh? what to do here?
//                caseBody = fastForward(pcase, ts, PTokenId.RBRACE);
            }
            nextSkipWhitespaceComment(ts);
            token = ts.token();
        }
    }
    private void parseIf(PElement parent, TokenSequence<PTokenId> ts, boolean includeElseIf) {
        PCondition cond = new PCondition(parent, ts.offset());
        nextSkipWhitespaceComment(ts);
        cond.setCondition(fastForward(cond, ts, false, PTokenId.LBRACE));
        nextSkipWhitespaceComment(ts);
        cond.setConsequence(fastForward(cond, ts, true, PTokenId.RBRACE));
        nextSkipWhitespaceComment(ts);
        Token<PTokenId> token = ts.token();
        while (token.id() == PTokenId.ELSE || (includeElseIf && token.id() == PTokenId.ELSIF)) {
            if (token.id() == PTokenId.ELSE) {
                nextSkipWhitespaceComment(ts);
                if (ts.token().id() == PTokenId.LBRACE) {
                    nextSkipWhitespaceComment(ts);
                } else {
                    //ignore?
                    return;
                }
                cond.setOtherwise(fastForward(cond, ts, true, PTokenId.RBRACE));
                return;
            } else {
                nextSkipWhitespaceComment(ts);
                PCondition par = cond;
                cond = new PCondition(par, ts.offset());
                par.setOtherwise(cond);
                PBlob caseExpr = fastForward(cond, ts, false, PTokenId.LBRACE);
                cond.setCondition(caseExpr);
                nextSkipWhitespaceComment(ts);
                cond.setConsequence(fastForward(cond, ts, true, PTokenId.RBRACE));
                nextSkipWhitespaceComment(ts);
                token = ts.token();
            }
        }
        //we've peeked ahead to see if there was any elsif or else, there wasn't now we need to backoff to make calling fastForward happy
        prevBackoffWhitespaceComment(ts);
    }

    private void parseFunction(PFunction pFunction, TokenSequence<PTokenId> ts) {
        fastForward(pFunction, ts, false, PTokenId.RPAREN);
    }

    //current token to be either [ or unrelated
    private void parseTypeRef(PTypeReference pTypeReference, String name, TokenSequence<PTokenId> ts) {
        pTypeReference.parseTypes(pTypeReference, name, ts);
    }

    static boolean matches (Token<PTokenId> token, PTokenId id) {
        return token != null && token.id() == id;
    }

    static boolean matches (Token<PTokenId> token, PTokenId[] ids) {
        return token != null && Arrays.asList(ids).contains(token.id());
    }

    private void parseReqList(PFunction reqFunc, TokenSequence<PTokenId> ts) {
        Token<PTokenId> token = ts.token();

        while (token != null && token.id() == PTokenId.IDENTIFIER) {
            if ("Class".equals(token.text().toString())) {
                if (parseClassReference(ts, reqFunc)) break;
            } else {
                PClassRef cr = new PClassRef(reqFunc, ts.offset());
                cr.setName(new PIdentifier(cr, ts.offset(), token.text().toString()));
            }
            token = nextSkipWhitespaceComment(ts);
            if (token != null && token.id() == PTokenId.COMMA) {
                token = nextSkipWhitespaceComment(ts);
            } else {
                break;
            }
        }
    }

    public boolean parseClassReference(TokenSequence<PTokenId> ts, PElement parent) {
        Token<PTokenId> token;
        token = nextSkipWhitespaceComment(ts);
        if (token != null && token.id() == PTokenId.LBRACKET) {
            token = nextSkipWhitespaceComment(ts);
            if (token != null && token.id() == PTokenId.STRING_LITERAL) {
                PClassRef cr = new PClassRef(parent, ts.offset());
                cr.setName(new PIdentifier(cr, ts.offset() + 1, token.text().toString().substring(1, token.text().toString().length() - 1)));
                token = nextSkipWhitespaceComment(ts);
                if (token != null && token.id() == PTokenId.RBRACKET) {
                    //good
                } else {
                    return true; //error hwo to report
                }
            } else {
                return true; //error hwo to report
            }
        } else {
            return true; //error hwo to report
        }
        return false;
    }

}
