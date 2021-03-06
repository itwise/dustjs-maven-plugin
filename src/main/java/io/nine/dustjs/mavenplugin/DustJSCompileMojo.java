package io.nine.dustjs.mavenplugin;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 *     HTML Template pre-compile for DustJS
 * </pre>
 *
 * @author chanwook
 */
@Mojo(name = "dustjsCompiler")
public class DustJSCompileMojo extends AbstractMojo {

    /**
     *
     */
    @Parameter(property = "dustjsCompiler.sourceDirectory", required = false)
    private String sourceDirectory;

    @Parameter(property = "dustjsCompiler.targetDirectory", required = false)
    private String targetDirectory;

    @Parameter(property = "dustjsCompiler.compileScript", required = false)
    private String compileScript;

    @Parameter(property = "dustjsCompiler.dustjsFile", required = true)
    private String dustjsFile;

    @Parameter(property = "dustjsCompiler.templateFileSuffix", required = true, defaultValue = "html")
    private String templateFileSuffix;

    private DustCompiler dustCompiler = new DustCompiler();

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("DustJS Pre-Compiler Source Directory: " + sourceDirectory + ", Target Directory: " + targetDirectory);

        // Step 1. Load to compile
        loadScript();

        // Step 2. Find template file and compile, then create compiled file
        Map<String, String> compiledTemplateMap = compileTemplate();
        if (compiledTemplateMap != null) {
            createCompiledFile(compiledTemplateMap);
            getLog().info("DustJS Pre-Compile OK!");
        } else {
            getLog().info("Do not exist folder, then do not execute next process!");
        }
    }

    protected void createCompiledFile(Map<String, String> compiledMap) {
        // Step 1. Directory cleaning..
        try {
            FileUtils.deleteDirectory(new File(targetDirectory));
            Files.createDirectories(Paths.get(targetDirectory));

            if (getLog().isDebugEnabled()) {
                getLog().debug("Cleansing target directory!(" + targetDirectory + ")");
            }
        } catch (IOException ioe) {
            throw new DustJSCompileException("Error when create compiled template file!", ioe);
        }

        // Step 2. Create file..
        for (Map.Entry<String, String> e : compiledMap.entrySet()) {
            try {
                String fileName = targetDirectory + File.separator + e.getKey() + ".js";
                Files.write(Paths.get(fileName), e.getValue().getBytes());
                if (getLog().isInfoEnabled()) {
                    getLog().info("Create compiled success!(path: " + fileName + ")");
                }
            } catch (IOException ioe) {
                throw new DustJSCompileException("Error when create compiled template file!", ioe);
            }
        }
    }

    protected Map<String, String> compileTemplate() {
        final File dir = new File(sourceDirectory);
        if (dir == null || !dir.isDirectory()) {
            // do not process anymore!
            return null;
        }

        if (getLog().isInfoEnabled()) {
            getLog().info("Load template source directory: " + sourceDirectory + " directory has " + dir.listFiles().length + " File.");
        }

        Map<String, String> compiledMap = new HashMap<>();
        for (File template : dir.listFiles()) {
            if (isCompilingType(template)) {
                final String templateKey = getTemplateKey(template);
                final String compiled = dustCompiler.compile(templateKey, template, getLog());

                if (getLog().isDebugEnabled()) {
                    getLog().debug("DustJS Template: " + templateKey + "\n" + compiled);
                }
                compiledMap.put(templateKey, compiled);
            } else {
                getLog().debug("Pass compiling!! [path: " + template.getAbsolutePath() + "]");
            }
        }
        return compiledMap;
    }

    /**
     * 컴파일이 가능한 파일인지 확인
     *
     * @param file
     * @return 컴파일이 가능한 템플릿 파일이면 true, 아니면 false 반환
     */
    private boolean isCompilingType(File file) {
        if (file == null || file.isDirectory()) {
            return false;
        } else if (!file.getName().endsWith(templateFileSuffix)) {
            return false;
        }
        return true;
    }

    protected void loadScript() {
        if (compileScript != null && compileScript.length() > 0) {
            dustCompiler.loadScript(compileScript);
        } else {
            dustCompiler.loadDefaultScript();
        }

        dustCompiler.loadScript(dustjsFile);
    }

    private String getTemplateKey(File template) {
        final String name = template.getName();
        return name.replaceAll(".html", "");
    }

    public void setCompileScript(String compileScript) {
        this.compileScript = compileScript;
    }

    public void setDustCompiler(DustCompiler dustCompiler) {
        this.dustCompiler = dustCompiler;
    }

    public void setSourceDirectory(String sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    public void setDustjsFile(String dustjsFile) {
        this.dustjsFile = dustjsFile;
    }
}
