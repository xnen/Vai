package io.improt.vai.util;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public class FileTreeBuilder {

    /**
     * Creates an ASCII tree representation of the given files relative to the context directory.
     *
     * @param context The base directory to which paths are relative.
     * @param files   Array of File objects to include in the tree.
     * @return A String representing the ASCII tree.
     * @throws IllegalArgumentException if any file is not under the context directory.
     */
    public static String createTree(File context, List<File> files) {
        if (!context.isDirectory()) {
            throw new IllegalArgumentException("Context must be a directory.");
        }

        // Normalize the context path
        String contextPath = context.getAbsolutePath();
        if (!contextPath.endsWith(File.separator)) {
            contextPath += File.separator;
        }

        // Map to hold the directory structure
        TreeNode root = getTreeNode(context, files, contextPath);

        // Build the string representation
        StringBuilder sb = new StringBuilder();
        sb.append(root.getName()).append("\n");
        List<TreeNode> children = root.getChildrenSorted();
        for (int i = 0; i < children.size(); i++) {
            TreeNode child = children.get(i);
            boolean isLast = (i == children.size() - 1);
            buildString(child, "", sb, isLast);
        }

        return sb.toString();
    }

    @NotNull
    private static TreeNode getTreeNode(File context, List<File> files, String contextPath) {
        TreeNode root = new TreeNode(context.getName() + File.separator);

        // Build the tree structure
        for (File file : files) {
            String[] parts = getStrings(contextPath, file);
            if (parts == null) {
                continue;
            }

            TreeNode current = root;
            for (String part : parts) {
                current = current.getOrCreateChild(part);
            }
        }
        return root;
    }

    private static String[] getStrings(String contextPath, File file) {
        String filePath = file.getAbsolutePath();

        if (!filePath.startsWith(contextPath)) {
            System.out.println("File " + filePath + " is not under the context directory " + contextPath);
            return null;
//            throw new IllegalArgumentException("File " + filePath + " is not under the context directory " + contextPath);
        }

        // Compute the relative path
        String relativePath = filePath.substring(contextPath.length());

        // Split the relative path into parts
        return relativePath.split(Pattern.quote(File.separator));
    }

    /**
     * Recursively builds the ASCII tree string.
     *
     * @param node    Current TreeNode.
     * @param prefix  String prefix for the current level.
     * @param sb      StringBuilder to accumulate the tree.
     * @param isLast  Boolean indicating if the node is the last child.
     */
    private static void buildString(TreeNode node, String prefix, StringBuilder sb, boolean isLast) {
        sb.append(prefix);
        sb.append(isLast ? "└─ " : "├─ ");
        sb.append(node.getName()).append("\n");

        List<TreeNode> children = node.getChildrenSorted();
        for (int i = 0; i < children.size(); i++) {
            TreeNode child = children.get(i);
            boolean last = (i == children.size() - 1);
            String newPrefix = prefix + (isLast ? "   " : "│  ");
            buildString(child, newPrefix, sb, last);
        }
    }

    /**
     * Helper class to represent each node in the tree.
     */
    private static class TreeNode {
        private final String name;
        private final Map<String, TreeNode> childrenMap;

        public TreeNode(String name) {
            this.name = name;
            this.childrenMap = new TreeMap<>();
        }

        public String getName() {
            return name;
        }

        public TreeNode getOrCreateChild(String childName) {
            return childrenMap.computeIfAbsent(childName, TreeNode::new);
        }

        public List<TreeNode> getChildrenSorted() {
            return new ArrayList<>(childrenMap.values());
        }
    }
}
