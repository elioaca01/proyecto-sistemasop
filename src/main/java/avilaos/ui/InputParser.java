package avilaos.ui;
public final class InputParser {
    private InputParser(){}
    public static int integer(String text,String field,int minimum){try{int n=Integer.parseInt(text==null?"":text.strip());if(n<minimum)throw new NumberFormatException();return n;}catch(NumberFormatException e){throw new IllegalArgumentException(field+": usa un entero entre "+minimum+" y "+Integer.MAX_VALUE);}}
    public static long longInteger(String text,String field,long minimum){try{long n=Long.parseLong(text==null?"":text.strip());if(n<minimum)throw new NumberFormatException();return n;}catch(NumberFormatException e){throw new IllegalArgumentException(field+": usa un entero entre "+minimum+" y "+Long.MAX_VALUE);}}
}
