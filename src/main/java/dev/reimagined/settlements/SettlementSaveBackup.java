package dev.reimagined.settlements;

import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.util.HexFormat;

/** One-time content-addressed copy before a legacy settlement file can be overwritten. */
public final class SettlementSaveBackup {
    private static final long MAX_BYTES=32L*1024*1024;
    private SettlementSaveBackup() {}
    private static String digest(Path path) throws IOException {
        if (Files.isSymbolicLink(path) || Files.size(path)>MAX_BYTES) throw new IOException("Unsafe/oversized settlement file: "+path);
        try {
            var hash=MessageDigest.getInstance("SHA-256");long count=0;
            try(var in=Files.newInputStream(path)) { byte[] buffer=new byte[8192];int n;
                while((n=in.read(buffer))!=-1) { count+=n;if(count>MAX_BYTES)throw new IOException("Settlement backup budget exceeded");hash.update(buffer,0,n); }
            }
            return HexFormat.of().formatHex(hash.digest());
        } catch(NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    public static Path preserve(Path source) throws IOException {
        String hash=digest(source);Path directory=source.resolveSibling("reimagined-settlement-backups");
        Files.createDirectories(directory);Path backup=directory.resolve(source.getFileName()+"."+hash+".pre-v2");
        if(Files.exists(backup)) {
            if(!digest(backup).equals(hash))throw new IOException("Existing backup is damaged; refusing migration: "+backup);
            return backup;
        }
        Path temporary=Files.createTempFile(directory,"settlement-", ".part");
        try {
            Files.copy(source,temporary,StandardCopyOption.REPLACE_EXISTING);
            if(!digest(temporary).equals(hash))throw new IOException("Settlement changed during backup; refusing migration");
            Files.move(temporary,backup);
        } finally { Files.deleteIfExists(temporary); }
        return backup;
    }
}
