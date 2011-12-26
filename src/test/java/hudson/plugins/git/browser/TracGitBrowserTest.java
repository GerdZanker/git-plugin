package hudson.plugins.git.browser;

import hudson.plugins.git.GitChangeLogParser;
import hudson.plugins.git.GitChangeSet;
import hudson.plugins.git.GitChangeSet.Path;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

/**
 * @author Gerd Zanker (gerd.zanker@web.de)
 * 
 * Based on the ViewGetWeb code from
 * @author Paul Nyheim (paul.nyheim@gmail.com)
 */
public class TracGitBrowserTest extends TestCase {

    private static final String TRAC_URL = "https://trac";
    private final TracGitBrowser tracGitBrowser;

    {
        try {
            tracGitBrowser = new TracGitBrowser(TRAC_URL);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test method for {@link hudson.plugins.git.browser.TracGitBrowser#getUrl()}.
     * Tast case to check if / was added to URL.
     * 
     * @throws MalformedURLException
     */
    public void testGetUrl() throws MalformedURLException {
        assertEquals(TRAC_URL + "/", String.valueOf(tracGitBrowser.getUrl()));
    }

    /**
     * Test method for {@link hudson.plugins.git.browser.TracGitBrowser#getUrl()}.
     * Test case where no / needs to be added.
     * 
     * @throws MalformedURLException
     */
    public void testGetUrlForRepoWithTrailingSlash() throws MalformedURLException {
        assertEquals(TRAC_URL + "/", String.valueOf(new TracGitBrowser(TRAC_URL + "/").getUrl()));
    }

    
    /**
     * Test method for
     * {@link hudson.plugins.git.browser.TracGitBrowser#getChangeSetLink(hudson.plugins.git.GitChangeSet)}.
     * Test case where a simple link to a changeset is build.
     * 
     * @throws SAXException
     * @throws IOException
     */
    public void testGetChangeSetLinkGitChangeSet() throws IOException, SAXException {
        final URL changeSetLink = tracGitBrowser.getChangeSetLink(createChangeSet("rawchangelog"));
        assertEquals(TRAC_URL+"/changeset/396fc230a3db05c427737aa5c2eb7856ba72b05d", changeSetLink.toString());
    }

    /**
     * Test method for
     * {@link hudson.plugins.git.browser.TracGitBrowser#getDiffLink(hudson.plugins.git.GitChangeSet.Path)}.
     * Test cases where links are tested, leading to the same changeset page as above, but using anchors.
     * The anchors are the diff sections for each file inside the changeset.
     * 
     * @throws SAXException
     * @throws IOException
     */
    public void testGetDiffLinkPath() throws IOException, SAXException {
        final HashMap<String, Path> pathMap = createPathMap("rawchangelog");
        final Path path1 = pathMap.get("src/main/java/hudson/plugins/git/browser/GithubWeb.java");
        assertEquals(TRAC_URL + "/changeset/396fc230a3db05c427737aa5c2eb7856ba72b05d#file0", tracGitBrowser.getDiffLink(path1).toString());
        final Path path2 = pathMap.get("src/test/java/hudson/plugins/git/browser/GithubWebTest.java");
        assertEquals(TRAC_URL + "/changeset/396fc230a3db05c427737aa5c2eb7856ba72b05d#file1", tracGitBrowser.getDiffLink(path2).toString());
        final Path path3 = pathMap.get("src/test/resources/hudson/plugins/git/browser/rawchangelog-with-deleted-file");
        assertNull("Do not return a diff link for added files.", tracGitBrowser.getDiffLink(path3));
    }

    /**
     * Test method for
     * {@link hudson.plugins.git.browser.TracGitBrowser#getDiffLink(hudson.plugins.git.GitChangeSet.Path)}.
     * Special test cases for a diff of a deleted file. 
     * Here no diff is available and therefore the test checks for null. 
     * 
     * @throws SAXException
     * @throws IOException
     */
   public void testGetDiffLinkForDeletedFile() throws Exception{
        final HashMap<String, Path> pathMap = createPathMap("rawchangelog-with-deleted-file");
        final Path path = pathMap.get("bar");
        assertNull("Do not return a diff link for deleted files.", tracGitBrowser.getDiffLink(path));

    }
    
    /**
     * Test method for
     * {@link hudson.plugins.git.browser.TracGitBrowser#getFileLink(hudson.plugins.git.GitChangeSet.Path)}.
     * Test case for a link to a file of one dedicated revision, derived from changeset
     * 
     * @throws SAXException
     * @throws IOException
     */
    public void testGetFileLinkPath() throws IOException, SAXException {
        final HashMap<String, Path> pathMap = createPathMap("rawchangelog");
        final Path path = pathMap.get("src/main/java/hudson/plugins/git/browser/GithubWeb.java");
        final URL fileLink = tracGitBrowser.getFileLink(path);
        assertEquals(TRAC_URL + "/browser/src/main/java/hudson/plugins/git/browser/GithubWeb.java?rev=396fc230a3db05c427737aa5c2eb7856ba72b05d",
                String.valueOf(fileLink));
    }
    
    /**
     * Test method for
     * {@link hudson.plugins.git.browser.TracGitBrowser#getFileLink(hudson.plugins.git.GitChangeSet.Path)}.
     * Special test case for a link to a deleted file of one dedicated revision, derived from changeset.
     * Here the changeset doesn't have the right ID, because the parent changeset ID must be used
     * to link to the last possible revision.
     * 
     * @throws SAXException
     * @throws IOException
     */
    public void testGetFileLinkPathForDeletedFile() throws IOException, SAXException {
        final HashMap<String, Path> pathMap = createPathMap("rawchangelog-with-deleted-file");
        final Path path = pathMap.get("bar");
        final URL fileLink = tracGitBrowser.getFileLink(path);
        assertEquals(TRAC_URL + "/browser/bar?rev=b547aa10c3f06710c6fdfcdb2a9149c81662923b", String.valueOf(fileLink));
    }
    
    

    private GitChangeSet createChangeSet(String rawchangelogpath) throws IOException, SAXException {
        final File rawchangelog = new File(TracGitBrowserTest.class.getResource(rawchangelogpath).getFile());
        final GitChangeLogParser logParser = new GitChangeLogParser(false);
        final List<GitChangeSet> changeSetList = logParser.parse(null, rawchangelog).getLogs();
        return changeSetList.get(0);
    }


    private HashMap<String, Path> createPathMap(final String changelog) throws IOException, SAXException {
        final HashMap<String, Path> pathMap = new HashMap<String, Path>();
        final Collection<Path> changeSet = createChangeSet(changelog).getPaths();
        for (final Path path : changeSet) {
            pathMap.put(path.getPath(), path);
        }
        return pathMap;
    }

}
