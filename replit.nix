{ pkgs }: {
    deps = [
        pkgs.graalvm17-ce
        pkgs.maven
        pkgs.nodejs
        pkgs.nodePackages.pnpm
    ];
}