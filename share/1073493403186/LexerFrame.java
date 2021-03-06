import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.BadLocationException;

import antlr.*;

/**
 * @author Santhosh Kumar T
 * @version 1.0
 */

public class LexerFrame extends JFrame implements ActionListener{
    JSplitPane jSplitPane1 = new JSplitPane();
    JScrollPane jScrollPane1 = new JScrollPane();
    JScrollPane jScrollPane2 = new JScrollPane();
    JTextPane tokenPane = new HScrollableTextPane();
    JTextArea scriptPane = new JTextArea();
    Border border1;
    Border border2;

    Class lexerClass;

    public LexerFrame(Class lexerClass, Class tokenTypesClass){
        super("Token Steam Viewer");
        this.lexerClass = lexerClass;
        try{
            jbInit();
            setSize(500, 500);
            listTokens(tokenTypesClass);

            final JPopupMenu popup = new JPopupMenu();
            popup.add(loadFileAction);

            scriptPane.addMouseListener(new MouseAdapter(){
                public void mouseReleased(MouseEvent e) {
                    if(e.isPopupTrigger())
                        popup.show(scriptPane, e.getX(), e.getY());
                }
            });
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    Hashtable tokens = new Hashtable();

    private void listTokens(Class tokenTypes) throws Exception{
        Field field[] = tokenTypes.getDeclaredFields();
        for(int i = 0; i<field.length; i++)
            tokens.put(field[i].get(null), field[i].getName());
    }

    public void actionPerformed(ActionEvent ae){
        Token token = (Token) ((JComponent) ae.getSource()).getClientProperty("token");
        if(token.getType()==Token.EOF_TYPE){
            scriptPane.select(0, 0);
            return;
        }
        try{
            int start = scriptPane.getLineStartOffset(token.getLine()-1)+token.getColumn()-1;
            scriptPane.select(start, start+token.getText().length());
            scriptPane.requestFocus();
        } catch(BadLocationException ex){
        }
    }

    private Action loadFileAction = new AbstractAction("Open File..."){
        public void actionPerformed(ActionEvent ae){
            JFileChooser jfc = new JFileChooser();
            int response = jfc.showOpenDialog(LexerFrame.this);
            if(response!=JFileChooser.APPROVE_OPTION)
                return;
            try{
                scanScript(jfc.getSelectedFile());
            } catch(Exception ex){
                ex.printStackTrace();
            }
        }
    };

    private void scanScript(File file) throws Exception{
        scriptPane.read(new FileReader(file), null);

        // create lexer
        Constructor constructor = lexerClass.getConstructor(new Class[]{InputStream.class});
        CharScanner lexer = (CharScanner) constructor.newInstance(new Object[]{new FileInputStream(file)});

        tokenPane.setEditable(true);
        tokenPane.setText("");

        int line = 1;
        ButtonGroup bg = new ButtonGroup();
        Token token = null;

        while(true){
            token = lexer.nextToken();
            JToggleButton tokenButton = new JToggleButton((String) tokens.get(new Integer(token.getType())));
            bg.add(tokenButton);
            tokenButton.addActionListener(this);
            tokenButton.setToolTipText(token.getText());
            tokenButton.putClientProperty("token", token);
            tokenButton.setMargin(new Insets(0, 1, 0, 1));
            tokenButton.setFocusPainted(false);
            if(token.getLine()>line){
                tokenPane.getDocument().insertString(tokenPane.getDocument().getLength(), "\n", null);
                line = token.getLine();
            }
            insertComponent(tokenButton);
            if(token.getType()==Token.EOF_TYPE)
                break;
        }

        tokenPane.setEditable(false);
        tokenPane.setCaretPosition(0);
    }

    private void insertComponent(JComponent comp){
        try{
            tokenPane.getDocument().insertString(tokenPane.getDocument().getLength(), " ", null);
        } catch(BadLocationException ex1){
        }
        try{
            tokenPane.setCaretPosition(tokenPane.getDocument().getLength()-1);
        } catch(Exception ex){
            tokenPane.setCaretPosition(0);
        }
        tokenPane.insertComponent(comp);
    }

    private void jbInit() throws Exception{
        border1 = BorderFactory.createEmptyBorder();
        border2 = BorderFactory.createEmptyBorder();
        jSplitPane1.setOrientation(JSplitPane.VERTICAL_SPLIT);
        tokenPane.setEditable(false);
        tokenPane.setText("");
        scriptPane.setFont(new java.awt.Font("DialogInput", 0, 12));
        scriptPane.setEditable(false);
        scriptPane.setMargin(new Insets(5, 5, 5, 5));
        scriptPane.setText("");
        jScrollPane1.setBorder(border1);
        jScrollPane2.setBorder(border1);
        this.getContentPane().add(jSplitPane1, BorderLayout.CENTER);
        jSplitPane1.add(jScrollPane1, JSplitPane.LEFT);
        jScrollPane1.getViewport().add(tokenPane, null);
        jSplitPane1.add(jScrollPane2, JSplitPane.RIGHT);
        jScrollPane2.getViewport().add(scriptPane, null);

        jScrollPane1.setColumnHeaderView(new JLabel(" Token Stream:"));
        jScrollPane2.setColumnHeaderView(new JLabel(" Input Script:"));
        jSplitPane1.setResizeWeight(0.5);
    }

    public static void main(String[] args) throws Exception{
        try{
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(Exception ignore){
        }
        new LexerFrame(SqlLexer.class, SqlTokenTypes.class).show();
    }
}


class HScrollableTextPane extends JTextPane{
    public boolean getScrollableTracksViewportWidth(){
        return(getSize().width<getParent().getSize().width);
    }

    public void setSize(Dimension d){
        if(d.width<getParent().getSize().width){
            d.width = getParent().getSize().width;
        }
        super.setSize(d);
    }
}