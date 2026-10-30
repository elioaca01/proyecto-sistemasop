package avilaos.config;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;

/** Escribe completo y cierra el temporal antes de sustituir atómicamente el destino. */
public final class JsonConfigurationStore implements ConfigurationStore {
    public SimulationConfig load(Path file)throws IOException{
        if(file==null)throw new IllegalArgumentException("Archivo requerido");
        var decoder=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        try(Reader reader=new BufferedReader(new InputStreamReader(Files.newInputStream(file),decoder))){return ScenarioJson.read(reader);}
        catch(ConfigurationException e){throw new ConfigurationException(file+": "+e.getMessage(),e);}
        catch(IOException e){throw new IOException(file+": "+e.getMessage(),e);}
    }
    public void save(Path file,SimulationConfig config)throws IOException{
        if(file==null||config==null)throw new IllegalArgumentException("Archivo y escenario requeridos");
        Path target=file.toAbsolutePath(),temporary=null;
        try{
            temporary=Files.createTempFile(target.getParent(),".avilaos-",".tmp");
            var encoder=StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
            try(Writer writer=new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(temporary),encoder))){ScenarioJson.write(writer,config);}
            try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){throw new IOException("El sistema de archivos no admite reemplazo atómico; se conserva el destino",e);}
        }catch(IOException e){throw new IOException(file+": "+e.getMessage(),e);}
        finally{if(temporary!=null)Files.deleteIfExists(temporary);}
    }
}
