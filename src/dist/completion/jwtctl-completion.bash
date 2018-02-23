#!/usr/bin/env bash

_jwtctl() {

    local DEFAULT_ARGS=("--help -h --verbose -v")
    local MAIN_ARGS=("create read --version")
    local CREATE_ARGS=("-c --claim -f --claims-file --header --headers-file -d --duration --deflate --gzip --hmac-sign --rsa-sign -p --password")
    local HMAC_SIGN_VALUES=("HS256 HS384 HS512")
    local RSA_SIGN_VALUES=("RS256 RS384 RS512 PS256 PS384 PS512")
    local READ_ARGS=("-s --secret -f --public-key-file --standard --json --ignore-expiration --ignore-signature")

    COMPREPLY=()
    local cur=${COMP_WORDS[COMP_CWORD]}

    local cmd=${COMP_WORDS[1]}

    local prev=${COMP_WORDS[COMP_CWORD-1]}

    case "$cmd" in
        create)
            case "$prev" in
                "--hmac-sign")
                    COMPREPLY=($(compgen -W "${HMAC_SIGN_VALUES}" -- ${cur}))
                    return 0
                ;;
                "--rsa-sign")
                    COMPREPLY=($(compgen -W "${HMAC_SIGN_VALUES}" -- ${cur}))
                    return 0
                ;;
                "RS256"|"RS384"|"RS512"|"PS256"|"PS384"|"PS512")
                    COMPREPLY=($(compgen -f ${cur}))
                    return 0
                ;;
                "--claims-file"|"-f"|"--headers-file")
                    COMPREPLY=($(compgen -f ${cur}))
                    return 0
                ;;
                *)
                    COMPREPLY=($(compgen -W "${CREATE_ARGS} ${DEFAULT_ARGS}" -- ${cur}))
                    return 0
                ;;
            esac
        ;;
        read)
            case "$prev" in
                "--public-key-file"|"-f")
                    COMPREPLY=($(compgen -f ${cur}))
                    return 0
                ;;
                *)
                    COMPREPLY=($(compgen -W "${READ_ARGS} ${DEFAULT_ARGS}" -- ${cur}))
                    return 0
                ;;
            esac
        ;;
        *)
        ;;
    esac

    COMPREPLY=($(compgen -W "${MAIN_ARGS} ${DEFAULT_ARGS}" -- ${cur}))
    return 0
}

complete -F _jwtctl jwtctl
