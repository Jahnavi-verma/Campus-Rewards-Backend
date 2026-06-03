{ pkgs }: {
  deps = [
    pkgs.maven
    pkgs.jdk17
    pkgs.nodejs-24_x
    pkgs.pnpm
    pkgs.netcat-gnu
  ];
}