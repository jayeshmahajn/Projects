// package com.converter.doctopdf.DocToPdf.config;

// import org.jodconverter.local.LocalConverter;
// import org.jodconverter.local.office.LocalOfficeManager;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.beans.factory.annotation.Value;

// import java.io.File;

// @Configuration
// public class LibreOfficeConfig {

//     @Value("${libreoffice.home:}")
//     private String libreOfficeHome;

//     @Bean
//     public LocalOfficeManager localOfficeManager() {
//         // Try to find LibreOffice installation
//         String officeHome = findLibreOfficeHome();

//         System.out.println("Using LibreOffice home: " + officeHome);

//         LocalOfficeManager.Builder builder = LocalOfficeManager.builder()
//                 .processTimeout(120000L)
//                 .processRetryInterval(250L)
//                 .maxTasksPerProcess(200)
//                 .taskExecutionTimeout(120000L)
//                 .taskQueueTimeout(30000L);

//         if (officeHome != null && !officeHome.isEmpty()) {
//             builder.officeHome(officeHome);
//         }

//         LocalOfficeManager manager = builder.build();

//         // Start the office manager
//         try {
//             manager.start();
//             System.out.println("LibreOffice manager started successfully");
//         } catch (Exception e) {
//             System.err.println("Failed to start LibreOffice manager: " + e.getMessage());
//             e.printStackTrace();
//         }

//         return manager;
//     }

//     @Bean
//     public LocalConverter documentConverter(LocalOfficeManager localOfficeManager) {
//         return LocalConverter.builder()
//                 .officeManager(localOfficeManager)
//                 .build();
//     }

//     private String findLibreOfficeHome() {
//         // Check if explicitly configured
//         if (libreOfficeHome != null && !libreOfficeHome.isEmpty()) {
//             return libreOfficeHome;
//         }

//         // Common LibreOffice installation paths
//         String[] possiblePaths = {
//                 "C:\\Program Files\\LibreOffice",
//                 "C:\\Program Files (x86)\\LibreOffice",
//                 "/usr/lib/libreoffice",
//                 "/opt/libreoffice",
//                 "/Applications/LibreOffice.app/Contents",
//                 System.getProperty("user.home") + "/LibreOffice"
//         };

//         for (String path : possiblePaths) {
//             File dir = new File(path);
//             if (dir.exists() && dir.isDirectory()) {
//                 System.out.println("Found LibreOffice at: " + path);
//                 return path;
//             }
//         }

//         System.out.println("LibreOffice not found in common locations, using system default");
//         return null; // Let JODConverter auto-detect
//     }
// }
package com.converter.doctopdf.DocToPdf.config;

import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Configuration
public class LibreOfficeConfig {

    private static final Logger logger = LoggerFactory.getLogger(LibreOfficeConfig.class);

    @Value("${jodconverter.local.office-home:}")
    private String officeHome;

    @Value("${jodconverter.local.port-numbers:2002}")
    private int[] portNumbers;

    @Value("${jodconverter.local.process-timeout:120000}")
    private long processTimeout;

    @Value("${jodconverter.local.process-retry-interval:1000}")
    private long processRetryInterval;

    @Value("${jodconverter.local.max-tasks-per-process:200}")
    private int maxTasksPerProcess;

    @Value("${jodconverter.local.task-execution-timeout:120000}")
    private long taskExecutionTimeout;

    @Value("${jodconverter.local.task-queue-timeout:30000}")
    private long taskQueueTimeout;

    @Bean(initMethod = "start", destroyMethod = "stop")
    public LocalOfficeManager localOfficeManager() {
        String detectedOfficeHome = findLibreOfficeHome();
        logger.info("Configuring LibreOffice manager with home: {}", detectedOfficeHome);

        LocalOfficeManager.Builder builder = LocalOfficeManager.builder()
                .portNumbers(portNumbers)
                .processTimeout(processTimeout)
                .processRetryInterval(processRetryInterval)
                .maxTasksPerProcess(maxTasksPerProcess)
                .taskExecutionTimeout(taskExecutionTimeout)
                .taskQueueTimeout(taskQueueTimeout);

        if (detectedOfficeHome != null && !detectedOfficeHome.isEmpty()) {
            builder.officeHome(detectedOfficeHome);
        }

        return builder.build();
    }

    @Bean
    public LocalConverter documentConverter(LocalOfficeManager localOfficeManager) {
        return LocalConverter.builder()
                .officeManager(localOfficeManager)
                .build();
    }

    private String findLibreOfficeHome() {
        // Use explicitly configured path if available
        if (officeHome != null && !officeHome.trim().isEmpty()) {
            if (new File(officeHome).exists()) {
                return officeHome;
            }
            logger.warn("Configured office home doesn't exist: {}", officeHome);
        }

        // Common installation paths (cross-platform)
        List<String> possiblePaths = Arrays.asList(
                "C:\\Program Files\\LibreOffice",
                "C:\\Program Files (x86)\\LibreOffice",
                "/usr/lib/libreoffice",
                "/opt/libreoffice",
                "/Applications/LibreOffice.app/Contents",
                System.getProperty("user.home") + "/LibreOffice");

        for (String path : possiblePaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                logger.info("Auto-detected LibreOffice at: {}", path);
                return path;
            }
        }

        logger.warn("LibreOffice not found in common locations. JODConverter will attempt auto-detection");
        return null;
    }

}