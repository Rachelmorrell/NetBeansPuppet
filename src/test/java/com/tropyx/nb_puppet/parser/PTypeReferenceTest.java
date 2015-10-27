package com.tropyx.nb_puppet.parser;

import com.tropyx.nb_puppet.lexer.PLangHierarchy;
import com.tropyx.nb_puppet.lexer.PTokenId;
import java.util.List;
import javax.swing.text.BadLocationException;
import org.junit.Test;
import static org.junit.Assert.*;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;

/**
 *
 * @author mkleint
 */
public class PTypeReferenceTest {

    public PTypeReferenceTest() {
    }


    @Test
    public void testInteger() throws BadLocationException {
        PTypeReference ref = parseTypeRef("Integer[1, 11]  xx");
        List<PNumber> params = ref.getChildrenOfType(PNumber.class, true);
        assertEquals(2, params.size());
        assertEquals(1, params.get(0).getValue());
        assertEquals(11, params.get(1).getValue());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Integer[22]  xx");
        params = ref.getChildrenOfType(PNumber.class, true);
        assertEquals(1, params.size());
        assertEquals(22, params.get(0).getValue());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Integer   xx");
        params = ref.getChildrenOfType(PNumber.class, true);
        assertEquals(0, params.size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Integer[default, -11]");
        params = ref.getChildrenOfType(PNumber.class, true);
        assertEquals(1, params.size());
        assertEquals(-11, params.get(0).getValue());
        assertEquals(1, ref.getChildrenOfType(PIdentifier.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Integer[default, 1, 1]");
        assertEquals(1, ref.getChildrenOfType(PNumber.class, true).size());
        assertEquals(1, ref.getChildrenOfType(PIdentifier.class, true).size());
        assertEquals(1, ref.getChildrenOfType(PError.class, true).size());


        ref = parseTypeRef("Integer[ddd, -11]  xx");
        params = ref.getChildrenOfType(PNumber.class, true);
        assertEquals(0, params.size());
        assertEquals(1, ref.getChildrenOfType(PError.class, true).size());

    }

    @Test
    public void testFloat() throws BadLocationException {
        PTypeReference ref = parseTypeRef("Float[1.1, -11.1]  xx");
        List<PFloat> params = ref.getChildrenOfType(PFloat.class, true);
        assertEquals(2, params.size());
        assertEquals(1.1, params.get(0).getValue(), 0.1);
        assertEquals(-11.1, params.get(1).getValue(), 0.1);
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Float[22.222]  xx");
        params = ref.getChildrenOfType(PFloat.class, true);
        assertEquals(1, params.size());
        assertEquals(22.222, params.get(0).getValue(), 0.1);
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Float   xx");
        params = ref.getChildrenOfType(PFloat.class, true);
        assertEquals(0, params.size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Float[default, -11.0]");
        params = ref.getChildrenOfType(PFloat.class, true);
        assertEquals(1, params.size());
        assertEquals(-11, params.get(0).getValue(), 0.1);
        assertEquals(1, ref.getChildrenOfType(PIdentifier.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Float[default, 1.1, 1.1]");
        params = ref.getChildrenOfType(PFloat.class, true);
        assertEquals(1, params.size());
        assertEquals(1, ref.getChildrenOfType(PIdentifier.class, true).size());
        assertEquals(1, ref.getChildrenOfType(PError.class, true).size());


        ref = parseTypeRef("Float[ddd, -11.0]  xx");
        params = ref.getChildrenOfType(PFloat.class, true);
        assertEquals(0, params.size());
        assertEquals(1, ref.getChildrenOfType(PError.class, true).size());

    }

    @Test
    public void testNumeric() throws BadLocationException {
        PTypeReference ref = parseTypeRef("Numeric[1, -11.1]  xx");
        List<PFloat> floats = ref.getChildrenOfType(PFloat.class, true);
        List<PNumber> nums = ref.getChildrenOfType(PNumber.class, true);
        assertEquals(1, floats.size());
        assertEquals(-11.1, floats.get(0).getValue(), 0.1);
        assertEquals(1, nums.size());
        assertEquals(1, nums.get(0).getValue());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

    }



    @Test
    public void testString() throws BadLocationException {
        PTypeReference ref = parseTypeRef("String[1, 11]  xx");
        List<PNumber> params = ref.getChildrenOfType(PNumber.class, true);
        assertEquals(2, params.size());
        assertEquals(1, params.get(0).getValue());
        assertEquals(11, params.get(1).getValue());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("String[22]  xx");
        params = ref.getChildrenOfType(PNumber.class, true);
        assertEquals(1, params.size());
        assertEquals(22, params.get(0).getValue());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("String   xx");
        params = ref.getChildrenOfType(PNumber.class, true);
        assertEquals(0, params.size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());


        ref = parseTypeRef("String[ddd, -11]  xx");
        params = ref.getChildrenOfType(PNumber.class, true);
        assertEquals(0, params.size());
        assertEquals(1, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("String[1.2]  xx");
        params = ref.getChildrenOfType(PNumber.class, true);
        assertEquals(0, params.size());
        assertEquals(1, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("String[]  xx");
        params = ref.getChildrenOfType(PNumber.class, true);
        assertEquals(0, params.size());
        assertEquals(1, ref.getChildrenOfType(PError.class, true).size());

    }

    @Test
    public void testArray() throws BadLocationException {
        PTypeReference ref = parseTypeRef("Array  xx");
        assertEquals(0, ref.getChildren().size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Array[String]  xx");
        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Array[Integer, 1]  xx");
        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(1, ref.getChildrenOfType(PNumber.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Array[Integer, 1, default]  xx");
        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(1, ref.getChildrenOfType(PNumber.class, true).size());
        assertEquals(1, ref.getChildrenOfType(PIdentifier.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Array[Integer, 1, default, 1]  xx");
        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(1, ref.getChildrenOfType(PNumber.class, true).size());
        assertEquals(1, ref.getChildrenOfType(PIdentifier.class, true).size());
        assertEquals(1, ref.getChildrenOfType(PError.class, true).size());
        //TODO
//        ref = parseTypeRef("Array[Variant[String, Integer]]  xx");
//        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
//        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

    }

    @Test
    public void testHash() throws BadLocationException {
        PTypeReference ref = parseTypeRef("Hash  xx");
        assertEquals(0, ref.getChildren().size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Hash[String]  xx"); //according to some docs this is illegal??
        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Hash[Integer, String, 1]  xx");
        assertEquals(2, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(1, ref.getChildrenOfType(PNumber.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Hash[Integer, String, 1, default]  xx");
        assertEquals(2, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(1, ref.getChildrenOfType(PNumber.class, true).size());
        assertEquals(1, ref.getChildrenOfType(PIdentifier.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Hash[Integer, 1, String, default, 1]  xx");
        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PNumber.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PIdentifier.class, true).size());
        assertEquals(1, ref.getChildrenOfType(PError.class, true).size());
        //TODO
//        ref = parseTypeRef("Hash[Variant[String, Integer], Enum['s']]  xx");
//        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
//        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

    }

    @Test
    public void testRegexp() throws BadLocationException {
        PTypeReference ref = parseTypeRef("Regexp  xx");
        assertEquals(0, ref.getChildren().size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Regexp[/xss/]  xx");
        assertEquals(1, ref.getChildrenOfType(PRegexp.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Regexp[sss]  xx");
        assertEquals(0, ref.getChildrenOfType(PRegexp.class, true).size());
        assertEquals(1, ref.getChildrenOfType(PError.class, true).size());
    }

    @Test
    public void testOptional() throws BadLocationException {
        PTypeReference ref = parseTypeRef("Optional  xx");
        //TODO param is mandatory
        assertEquals(0, ref.getChildren().size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Optional['aaa']  xx");
        assertEquals(1, ref.getChildrenOfType(PString.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Optional[String]  xx");
        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        //TODO
//        ref = parseTypeRef("Optional[Array[Integer[0, 10]]]  xx");
//        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
//        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());
    }

    @Test
    public void testNoUndef() throws BadLocationException {

        PTypeReference ref = parseTypeRef("NotUndef  xx");
        assertEquals(0, ref.getChildren().size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("NotUndef['aaa']  xx");
        assertEquals(1, ref.getChildrenOfType(PString.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("NotUndef[String]  xx");
        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        //TODO
//        ref = parseTypeRef("NotUndef[Array[Integer[0, 10]]]  xx");
//        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
//        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

    }

    @Test
    public void testVariant() throws BadLocationException {

        PTypeReference ref = parseTypeRef("Variant  xx");
        //TODO mandatory
        assertEquals(0, ref.getChildren().size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Variant[String]  xx");
        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Variant[String, Boolean]  xx");
        assertEquals(2, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        //TODO
//        ref = parseTypeRef("Variant[Array[Integer[0, 10]], Class['xxx']]  xx");
//        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
//        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

    }

    @Test
    public void testPattern() throws BadLocationException {

        PTypeReference ref = parseTypeRef("Pattern  xx");
        //TODO mandatory
        assertEquals(0, ref.getChildren().size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Pattern[/ddd/]  xx");
        assertEquals(1, ref.getChildrenOfType(PRegexp.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Pattern[/aaa/, /bbb/]  xx");
        assertEquals(2, ref.getChildrenOfType(PRegexp.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());
    }

    @Test
    public void testTuple() throws BadLocationException {

        PTypeReference ref = parseTypeRef("Tuple  xx");
        //TODO mandatory
        assertEquals(0, ref.getChildren().size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Tuple[String]  xx");
        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Tuple[String, Boolean]  xx");
        assertEquals(2, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        //TODO
//        ref = parseTypeRef("Tuple[String, Boolean, 1, 5]  xx");
//        assertEquals(2, ref.getChildrenOfType(PTypeReference.class, true).size());
//        assertEquals(2, ref.getChildrenOfType(PNumber.class, true).size());
//        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());
    }

    @Test
    public void testType() throws BadLocationException {

        PTypeReference ref = parseTypeRef("Type  xx");
        //TODO mandatory
        assertEquals(0, ref.getChildren().size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Type[String]  xx");
        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Type[String, Boolean]  xx");
        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(1, ref.getChildrenOfType(PError.class, true).size());

    }

    @Test
    public void testClass() throws BadLocationException {

        PTypeReference ref = parseTypeRef("Class  xx");
        assertEquals(0, ref.getChildren().size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Class['apache']  xx");
        assertEquals(1, ref.getChildrenOfType(PClassRef.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Class['apache', 'apache::install']   xxx");
        assertEquals(1, ref.getChildrenOfType(PError.class, true).size());
        assertEquals(1, ref.getChildrenOfType(PClassRef.class, true).size());
    }

    @Test
    public void testResource() throws BadLocationException {

        PTypeReference ref = parseTypeRef("File  xx");
        assertEquals(0, ref.getChildren().size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("File['/tmp/foo']  xx");
        assertEquals(1, ref.getChildrenOfType(PResourceRef.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("File['/tmp/foo', 'tmp/bar']   xxx");
        assertEquals(2, ref.getChildrenOfType(PResourceRef.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Resource[File]  xx");
        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());
        
        ref = parseTypeRef("Resource[File, '/tmp/foo']  xx");
        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(1, ref.getChildrenOfType(PResourceRef.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Resource[File, '/tmp/foo', '/tmp/bar']  xx");
        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals(2, ref.getChildrenOfType(PResourceRef.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Resource['file', '/tmp/foo']  xx");
        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals("File", ref.getChildrenOfType(PTypeReference.class, true).get(0).getType());
        assertEquals(1, ref.getChildrenOfType(PResourceRef.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

        ref = parseTypeRef("Resource['core::wget', '/tmp/foo']  xx");
        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
        assertEquals("Core::Wget", ref.getChildrenOfType(PTypeReference.class, true).get(0).getType());
        assertEquals(1, ref.getChildrenOfType(PResourceRef.class, true).size());
        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());
        //TODO
//        ref = parseTypeRef("Type[Resource['file']]  xx");
//        assertEquals(1, ref.getChildrenOfType(PTypeReference.class, true).size());
//        assertEquals(1, ref.getChildrenOfType(PString.class, true).size());
//        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());
//
//        ref = parseTypeRef("Type[Resource[File]]  xx");
//        assertEquals(2, ref.getChildrenOfType(PTypeReference.class, true).size());
//        assertEquals(0, ref.getChildrenOfType(PString.class, true).size());
//        assertEquals(0, ref.getChildrenOfType(PError.class, true).size());

    }




    public PTypeReference  parseTypeRef(String string) throws BadLocationException {
        final BaseDocument bd = new BaseDocument(false, "text/x-puppet-manifest");
        bd.insertString(0, string, null);
        final PTypeReference[] fRef = new PTypeReference[1];
        bd.render(new Runnable() {

            @Override
            public void run() {
                TokenSequence<PTokenId> ts = PLangHierarchy.getTokenSequence(bd);
                ts.moveStart();
                ts.moveNext();
                Token<PTokenId> token = ts.token();
                PTypeReference ref = new PTypeReference(null, 0, token.text().toString());
                PuppetParser.nextSkipWhitespaceComment(ts);
                ref.parseTypes(ref, token.text().toString(), ts);
                fRef[0] = ref;
            }
        });
        return fRef[0];
    }

}
