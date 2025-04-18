package hudson.plugins.msbuild;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.remoting.VirtualChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MsBuildInstallerTest {

    private MsBuildInstaller installer;

    @Mock
    private Node mockNode;
    @Mock
    private Computer mockComputer;
    @Mock
    private EnvVars mockEnvVars;
    @Mock
    private FilePath mockFilePath;
    @Mock
    private FilePath mockConfigFilePath;
    @Mock
    private FilePath vsConfigFilePath;
    @Mock
    private VirtualChannel mockChannel;

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        installer = new MsBuildInstaller("testLabel");
        mockChannel = mock(VirtualChannel.class);
        when(mockNode.getChannel()).thenReturn(mockChannel);
        when(mockFilePath.child("config.json")).thenReturn(mockConfigFilePath);
        when(mockFilePath.child(".vsconfig")).thenReturn(vsConfigFilePath);
        when(mockNode.toComputer()).thenReturn(mockComputer);
        when(mockComputer.getEnvironment()).thenReturn(mockEnvVars);
        when(mockEnvVars.containsKey("OS")).thenReturn(true);
        when(mockEnvVars.get("OS")).thenReturn("Windows_NT");
    }

    @Test
    void testBuildToolsInstallPathWithGivenPath() {
        String givenInstallPath = "D:\\Custom\\BuildTools";
        FilePath result = MsBuildInstaller.buildToolsInstallPath(mockNode, "2019", givenInstallPath);
        assertEquals(new FilePath(mockChannel, givenInstallPath), result);
    }

    @Test
    void testBuildToolsInstallPathWithoutGivenPath() {
        String selectedVersion = "2019";
        FilePath result = MsBuildInstaller.buildToolsInstallPath(mockNode, selectedVersion, null);
        assertEquals(
                new FilePath(mockChannel,
                        "C:\\Program Files (x86)\\Microsoft Visual Studio\\" + selectedVersion + "\\BuildTools\\"),
                result);
    }

    @Test
    void testMsBuildBinPathWithGivenPath() {
        String givenInstallPath = "D:\\Custom\\BuildTools";
        FilePath result = MsBuildInstaller.msBuildBinPath(mockNode, "2019", givenInstallPath);
        assertEquals(new FilePath(mockChannel, givenInstallPath).child("\\MSBuild\\Current\\Bin"), result);
    }

    @Test
    void testMsBuildBinPathWithoutGivenPath() {
        String selectedVersion = "2019";
        FilePath result = MsBuildInstaller.msBuildBinPath(mockNode, selectedVersion, null);
        assertEquals(new FilePath(mockChannel,
                "C:\\Program Files (x86)\\Microsoft Visual Studio\\" + selectedVersion + "\\BuildTools\\")
                .child("\\MSBuild\\Current\\Bin"), result);
    }

    @Test
    void testGetVs_BuildToolsExePath() {
        FilePath expected = mockFilePath;
        FilePath result = MsBuildInstaller.getVs_BuildToolsExePath(expected);
        assertEquals(expected.child("vs_BuildTools.exe"), result);
    }

    @Test
    void testExtractInstallPath_WhenGivenArgumentsIsNull_ReturnsNull() {
        String givenArguments = null;

        String result = MsBuildInstaller.extractInstallPath(givenArguments);

        assertNull(result);
    }

    @Test
    void testExtractInstallPath_WhenGivenArgumentsDoesNotContainInstallPath_ReturnsNull() {
        String givenArguments = "--quiet --norestart";

        String result = MsBuildInstaller.extractInstallPath(givenArguments);

        assertNull(result);
    }

    @Test
    void testExtractInstallPath_WhenGivenArgumentsContainInstallPath_ReturnsInstallPathValue() {
        String givenArguments = "--quiet --installPath \"C:\\Program Files\\MSBuild\"";

        String result = MsBuildInstaller.extractInstallPath(givenArguments);

        assertEquals("C:\\Program Files\\MSBuild", result);
    }

    @Test
    void testExtractInstallPath_WhenGivenArgumentsContainMultipleInstallPaths_ReturnsFirstInstallPathValue() {
        String givenArguments = "--quiet --installPath \"C:\\Program Files\\MSBuild\" --installPath D:\\MSBuild";

        String result = MsBuildInstaller.extractInstallPath(givenArguments);

        assertEquals("C:\\Program Files\\MSBuild", result);
    }

    @Test
    void testSetAndGetSelectedVersion() {
        installer.setSelectedVersion("2022");
        assertEquals("2022", installer.getSelectedVersion());
    }

    @Test
    void testSetAndGetAdditionalArguments() {
        installer.setAdditionalArguments("--quiet --norestart");
        assertEquals("--quiet --norestart", installer.getAdditionalArguments());
    }

    @Test
    void testSetAndGetVsconfig() {
        installer.setVsconfig("{ \"version\": \"16.0\" }");
        assertEquals("{ \"version\": \"16.0\" }", installer.getVsconfig());
    }

    @Test
    void testEnsureArguments() {
        String initialArgs = "--quiet";
        String[] argsToAdd = new String[] { "--wait", "--norestart" };
        String expected = "--quiet --wait --norestart";
        assertEquals(expected, MsBuildInstaller.ensureArguments(initialArgs, argsToAdd));
    }

    @Test
    void testCheckIfOsIsWindows() throws IOException, InterruptedException {
        when(mockNode.toComputer()).thenReturn(mockComputer);
        when(mockComputer.getEnvironment()).thenReturn(mockEnvVars);
        when(mockEnvVars.containsKey("OS")).thenReturn(true);
        when(mockEnvVars.get("OS")).thenReturn("Windows_NT");

        assertTrue(MsBuildInstaller.checkIfOsIsWindows(mockNode));

        when(mockEnvVars.get("OS")).thenReturn("Linux");
        assertFalse(MsBuildInstaller.checkIfOsIsWindows(mockNode));
    }

    @Test
    void testUseConfigFileWithNullVsConfig() throws IOException, InterruptedException {
        boolean result = MsBuildInstaller.useConfigFile(null, mockFilePath);
        verify(vsConfigFilePath, times(1)).delete();
        verifyNoMoreInteractions(vsConfigFilePath);
        assertFalse(result);
    }

    @Test
    void testUseConfigFileWithEmptyVsConfig() throws IOException, InterruptedException {
        boolean result = MsBuildInstaller.useConfigFile("", mockFilePath);
        verify(vsConfigFilePath, times(1)).delete();
        verifyNoMoreInteractions(vsConfigFilePath);
        assertFalse(result);
    }

    @Test
    void testUseConfigFileNonExistingFile() throws IOException, InterruptedException {
        when(vsConfigFilePath.exists()).thenReturn(false);

        boolean result = MsBuildInstaller.useConfigFile("some config", mockFilePath);

        verify(vsConfigFilePath, times(1)).write("some config", "UTF-8");
        assertTrue(result);
    }

    @Test
    void testUseConfigFileExistingFileDifferentContent() throws IOException, InterruptedException {
        when(vsConfigFilePath.exists()).thenReturn(true);
        when(vsConfigFilePath.readToString()).thenReturn("old config");

        boolean result = MsBuildInstaller.useConfigFile("new config", mockFilePath);

        verify(vsConfigFilePath, times(1)).write("new config", "UTF-8");
        assertTrue(result);
    }

    @Test
    void testNeedsModify() throws Exception {
        when(mockConfigFilePath.exists()).thenReturn(true);
        when(mockConfigFilePath.readToString()).thenReturn("{\"needsModify\":true}");

        boolean result = MsBuildInstaller.needsModify(mockFilePath);

        assertTrue(result, "The method should return true as the 'needsModify' key is set to true");
    }

    @Test
    void testNeedsUpdate_WhenLastUpdatedWithin24Hours_ReturnsFalse() throws IOException, InterruptedException {
        when(mockFilePath.child("config.json")).thenReturn(mockFilePath);
        when(mockFilePath.exists()).thenReturn(true);
        long currentTimestamp = System.currentTimeMillis() / 1000;
        when(mockFilePath.readToString()).thenReturn("{\"lastUpdated\":" + currentTimestamp + "}");

        boolean result = MsBuildInstaller.needsUpdate(mockFilePath);

        assertFalse(result, "The method should return false as the lastUpdated time is within 24 hours");
    }

    @Test
    void testNeedsUpdate_WhenLastUpdatedMoreThan24Hours_ReturnsTrue() throws IOException, InterruptedException {
        when(mockFilePath.child("config.json")).thenReturn(mockFilePath);
        when(mockFilePath.exists()).thenReturn(true);
        when(mockFilePath.readToString()).thenReturn("{\"lastUpdated\":1619300929}");

        boolean result = MsBuildInstaller.needsUpdate(mockFilePath);

        assertTrue(result, "The method should return true as the lastUpdated time is more than 24 hours");
    }

    @Test
    void testDownloadFile_SuccessfulDownload() throws Exception {
        URI mockURI = new URI("https://aka.ms/vs/17/release/vs_buildtools.exe");
        when(mockFilePath.write()).thenReturn(mock(OutputStream.class)); // Mocking OutputStream correctly

        MsBuildInstaller.downloadFile(mockURI, mockFilePath);
        verify(mockFilePath, times(1)).write();
    }

    @Test
    void testGetUrlForVersion() {
        assertEquals("https://aka.ms/vs/17/release/vs_buildtools.exe",
                MsBuildInstaller.DescriptorImpl.getUrlForVersion("2022"));
        assertEquals("https://aka.ms/vs/16/release/vs_buildtools.exe",
                MsBuildInstaller.DescriptorImpl.getUrlForVersion("2019"));
    }

    @Test
    void testGetDisplayName() {
        MsBuildInstaller.DescriptorImpl descriptor = new MsBuildInstaller.DescriptorImpl();
        String displayName = descriptor.getDisplayName();
        assertEquals("Install from Microsoft", displayName);
    }

    @Test
    void testIsApplicableWithCorrectType() {
        MsBuildInstaller.DescriptorImpl descriptor = new MsBuildInstaller.DescriptorImpl();
        assertTrue(descriptor.isApplicable(MsBuildInstallation.class));
    }
}
