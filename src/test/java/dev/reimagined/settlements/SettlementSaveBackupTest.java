package dev.reimagined.settlements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class SettlementSaveBackupTest {
 @TempDir Path dir;
 @Test void originalIsPreservedAndRepeatDoesNotDuplicateBackup() throws Exception {
  Path file=dir.resolve("reimagined_settlements.dat");byte[] bytes={1,2,3,4};Files.write(file,bytes);
  Path backup=SettlementSaveBackup.preserve(file);assertArrayEquals(bytes,Files.readAllBytes(backup));
  assertEquals(backup,SettlementSaveBackup.preserve(file));assertArrayEquals(bytes,Files.readAllBytes(file));
 }
 @Test void changedOriginalGetsAnotherBackupWithoutReplacingEarlier() throws Exception {
  Path file=dir.resolve("reimagined_settlements.dat");Files.writeString(file,"old");Path one=SettlementSaveBackup.preserve(file);
  Files.writeString(file,"changed");Path two=SettlementSaveBackup.preserve(file);
  assertNotEquals(one,two);assertEquals("old",Files.readString(one));assertEquals("changed",Files.readString(two));
 }
 @Test void damagedBackupBlocksMigrationAndRetainsOriginal() throws Exception {
  Path file=dir.resolve("reimagined_settlements.dat");Files.writeString(file,"original");Path backup=SettlementSaveBackup.preserve(file);
  Files.writeString(backup,"damaged");assertThrows(java.io.IOException.class,()->SettlementSaveBackup.preserve(file));
  assertEquals("original",Files.readString(file));
 }
}
