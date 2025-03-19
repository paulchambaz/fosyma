{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
  }:
    flake-utils.lib.eachDefaultSystem (system: let
      pkgs = nixpkgs.legacyPackages.${system};

      buildPkgs = with pkgs; [
        pkg-config
        scdoc
        fyne
        libGL
        xorg.libXxf86vm
        xorg.libX11.dev
        xorg.libXcursor
        xorg.libXi
        xorg.libXinerama
        xorg.libXrandr
        xorg.libXxf86vm
        libxkbcommon
        wayland
      ];

      libPkgs = with pkgs; [
      ];

      devPkgs = with pkgs; [
        maven
        jdk21
        just
        google-java-format
      ];
    in {
      devShell = pkgs.mkShell {
        nativeBuildInputs = buildPkgs;
        buildInputs = libPkgs ++ devPkgs;

        shellHook = ''
          export LD_LIBRARY_PATH=${pkgs.libGL}/lib:${pkgs.xorg.libXxf86vm}/lib:$LD_LIBRARY_PATH
        '';
      };
    });
}
