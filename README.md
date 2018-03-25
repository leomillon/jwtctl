# jwtctl

[![Build Status](https://travis-ci.org/leomillon/jwtctl.svg?branch=master)](https://travis-ci.org/leomillon/jwtctl)
[![GitHub (pre-)release](https://img.shields.io/github/release/leomillon/jwtctl/all.svg)](https://github.com/leomillon/jwtctl/releases)
[![GitHub license](https://img.shields.io/github/license/leomillon/jwtctl.svg)](https://github.com/leomillon/jwtctl/blob/master/LICENSE.md)
[![codebeat badge](https://codebeat.co/badges/8137ac27-6cce-496d-81cd-8ed414e94b71)](https://codebeat.co/projects/github-com-leomillon-jwtctl-master)

A command line tool to read or create [JWT](https://jwt.io/) tokens written in [Kotlin](https://kotlinlang.org/).

## Overview

You can easily create a JWT token :

```
$ jwtctl create --claim username john.doe --duration PT2H

eyJhbGciOiJub25lIn0.eyJpYXQiOjE1MTk0ODc3NzYsImV4cCI6MTUxOTQ5NDk3NiwidXNlcm5hbWUiOiJqb2huLmRvZSJ9.
```

You can read it :
```
$ jwtctl read eyJhbGciOiJub25lIn0.eyJpYXQiOjE1MTk0ODc3NzYsImV4cCI6MTUxOTQ5NDk3NiwidXNlcm5hbWUiOiJqb2huLmRvZSJ9.

{iat=1519487776, exp=1519494976, username=john.doe}
```

## Installation

### Via Homebrew

You can tap the dedicated repository [homebrew-jwtctl](https://github.com/leomillon/homebrew-jwtctl) to install jwtctl.

Just use the following commands :
```
brew tap leomillon/jwtctl
brew install jwtctl
```

> :information_source: Bash / ZSH auto completion will be installed but may not be activated/loaded.
Please follow the instructions written during the homebrew installation or jump the the manual installation section.

### Manual installation

First step, you have to download the latest release (`jwtctl-VERSION.zip` or `jwtctl-VERSION.tar` files) on this [page](https://github.com/leomillon/jwtctl/releases/latest).

#### Linux / Mac OS X

Once the archive has been uncompressed, you will find the `jwtctl` script in the `bin` directory.

Go into the `bin` directory and try the following command to get the global help :
```
./jwtctl --help
```

#### Auto completion Bash / ZSH

You can source the script `jwtctl-completion.bash` in the `completion` directory :
```
source completion/jwtctl-completion.bash
```

If you want enable it permanently, you will have to load it with your profile. Add the following line to your `~/.bash_profile` or `~/.zshrc` and replace `PATH_TO_JWTCTL` with the location of the jwtctl uncompressed folder.
```
[ -f PATH_TO_JWTCTL/jwtctl-completion.bash ] && source PATH_TO_JWTCTL/bash_completion.d/jwtctl-completion.bash
```

:information_source: ZSH users : you may need to add these two lines before the previous one to enable the bash completion :
```
autoload -U compinit && compinit
autoload -U bashcompinit && bashcompinit
```

#### Windows

Once the archive has been uncompressed, you will find the `jwtctl.bat` script in the `bin` directory.

Go into the `bin` directory and try the following command to get the global help :
```
jwtctl.bat --help
```

## Usage

### Get help

To get the global help :
```
$ jwtctl --help
usage: jwtctl [-h] [-v] [--debug] [--version] COMMAND [ARGS]...

tool used to read or create JWT tokens. See more info at https://jwt.io/

optional arguments:
  -h, --help  show this help message and exit

  -v,         enable verbose mode
  --verbose

  --debug     enable debug mode

  --version   show program version and exit


positional arguments:
  COMMAND     the command to excecute : [create, read]

  ARGS        the command args


'jwtctl COMMAND --help' to read about a specific command
```

### Commands

To get a specific command help, use the following pattern : `jwtctl COMMAND --help`

There are two commands :

- create : `jwtctl create --help`
- read : `jwtctl read --help`

You can always add the `--verbose` argument to display headers/body.

#### Create use-cases

##### JWT with expiration date

Creates a token containing a claim named `username` with `john.doe` as value. It will be expired in 2 hours from now.
```
jwtctl create --claim username john.doe --duration PT2H --verbose
```
Result :
```
INFO  | Verbose mode enabled
INFO  | Header  : {alg=none}
INFO  | Body    : {iat=1519491614, exp=1519498814, username=john.doe}
INFO  | Generated token until : 2018-02-24T19:00:14Z
eyJhbGciOiJub25lIn0.eyJpYXQiOjE1MTk0OTE2MTQsImV4cCI6MTUxOTQ5ODgxNCwidXNlcm5hbWUiOiJqb2huLmRvZSJ9.
```

##### JWT from JSON file

Creates a token from a JSON file and add the claim named `username` with `john.doe` as value.
```
jwtctl create --claims-file ./claims.json --claim username john.doe --verbose
```
Result :
```
INFO  | Verbose mode enabled
INFO  | Header  : {alg=none}
INFO  | Body    : {iat=1519493118, fileClaimsName=fileClaimsValue, username=john.doe}
INFO  | Generated token until : no expiration date
eyJhbGciOiJub25lIn0.eyJpYXQiOjE1MTk0OTMxMTgsImZpbGVDbGFpbXNOYW1lIjoiZmlsZUNsYWltc1ZhbHVlIiwidXNlcm5hbWUiOiJqb2huLmRvZSJ9.
```

##### JWS with HMAC (signed JWT)

Creates a token containing a claim named `username` with `john.doe` as value. It will be signed with an HMAC algorithm and a base 64 encoded secret.
```
jwtctl create --claim username john.doe --hmac-sign HS512 mysecret --verbose
```
Result :
```
INFO  | Verbose mode enabled
INFO  | Header  : {alg=HS512}
INFO  | Body    : {iat=1519491764, username=john.doe}
INFO  | Generated token until : no expiration date
eyJhbGciOiJIUzUxMiJ9.eyJpYXQiOjE1MTk0OTE3NjQsInVzZXJuYW1lIjoiam9obi5kb2UifQ.vM9WT8v3Ou0Tb_mVWkYBnf8yH5f8PAajttLpm5ucdANmY-ao_WS3KWDQzng9N8ykORgkYDtIOYauli5mTKxKLw
```

##### JWS with RSA (signed JWT)

Creates a token containing a claim named `username` with `john.doe` as value. It will be signed with an RSA algorithm and a private key (`PEM` file).
```
jwtctl create --claim username john.doe --rsa-sign RS512 ./private_key_1.pem --verbose
```
Result :
```
INFO  | Verbose mode enabled
Enter password (./private_key_1.pem) :
INFO  | Header  : {alg=RS512}
INFO  | Body    : {iat=1519492016, username=john.doe}
INFO  | Generated token until : no expiration date
eyJhbGciOiJSUzUxMiJ9.eyJpYXQiOjE1MTk0OTIwMTYsInVzZXJuYW1lIjoiam9obi5kb2UifQ.m-Ujo-2xK2xr8wC2R865kxlgU4UwL5tzha8yg8Eg-AdGGMti_ImsEe3MB19q7snPEYQkmcz6tRqdgN8oc-dIWyaGeTp7pnEIS2q4BoG_1ucFqqr5Ps7cwyZP-uFbYg_4tn6rtLmQrPLbf3oWoZTvOFu8BBAwkI6rH-9GI_vOQ9879UdA7FCV7l9B0J1KaQEXxkao0jFRywm_GFGWlIRVAa-yMWqQfUeP8V5H68TYw8L0pAhKK94SKV5wZ-UWPuVsCjaYamX1KCl5ECtfrlZfsuco87LrtXj9x-RBadAHbADgQFTChTEj-uN4q5a00IwZkiTHCdT9issnZWTxJIKjeg
```

If your private key is encrypted, you will be asked interactively to provide the password. If you want to give it as a command argument, just use `--password PRIVATE_KEY_PASSWORD`.

#### Read use-cases

You can always add the `--json` argument to display the body result as valid json.

##### JWT

Read a simple JWT (no signature, no expiration date).
```
jwtctl read eyJhbGciOiJub25lIn0.eyJpYXQiOjE1MTk0OTIzNDEsInVzZXJuYW1lIjoiam9obi5kb2UifQ. --verbose
```
Result :
```
INFO  | Verbose mode enabled
INFO  | Header  : {alg=none}
INFO  | Body    : {iat=1519492341, username=john.doe}
INFO  | Expired : false
{iat=1519492341, username=john.doe}
```

##### JWS with secret

Read a JWS providing the secret.
```
jwtctl read eyJhbGciOiJIUzUxMiJ9.eyJpYXQiOjE1MTk0OTE3NjQsInVzZXJuYW1lIjoiam9obi5kb2UifQ.vM9WT8v3Ou0Tb_mVWkYBnf8yH5f8PAajttLpm5ucdANmY-ao_WS3KWDQzng9N8ykORgkYDtIOYauli5mTKxKLw --secret mysecret --verbose
```
Result :
```
INFO  | Verbose mode enabled
INFO  | Header  : {alg=HS512}
INFO  | Body    : {iat=1519491764, username=john.doe}
INFO  | Expired : false
{iat=1519491764, username=john.doe}
```

##### JWS with public key

Read a JWS providing the public key `PEM` file.
```
jwtctl read eyJhbGciOiJSUzUxMiJ9.eyJpYXQiOjE1MTk0OTIwMTYsInVzZXJuYW1lIjoiam9obi5kb2UifQ.m-Ujo-2xK2xr8wC2R865kxlgU4UwL5tzha8yg8Eg-AdGGMti_ImsEe3MB19q7snPEYQkmcz6tRqdgN8oc-dIWyaGeTp7pnEIS2q4BoG_1ucFqqr5Ps7cwyZP-uFbYg_4tn6rtLmQrPLbf3oWoZTvOFu8BBAwkI6rH-9GI_vOQ9879UdA7FCV7l9B0J1KaQEXxkao0jFRywm_GFGWlIRVAa-yMWqQfUeP8V5H68TYw8L0pAhKK94SKV5wZ-UWPuVsCjaYamX1KCl5ECtfrlZfsuco87LrtXj9x-RBadAHbADgQFTChTEj-uN4q5a00IwZkiTHCdT9issnZWTxJIKjeg --public-key-file ./public_key_1.pem --verbose
```
Result :
```
INFO  | Verbose mode enabled
INFO  | Header  : {alg=RS512}
INFO  | Body    : {iat=1519492016, username=john.doe}
INFO  | Expired : false
{iat=1519492016, username=john.doe}
```

##### JWS ignoring signature and expiration 

Read a JWS ignoring signature and expiration.
```
jwtctl read eyJhbGciOiJIUzUxMiJ9.eyJpYXQiOjE1MTk0OTI4MzIsImV4cCI6MTUxOTQ5MjgzMywidXNlcm5hbWUiOiJqb2huLmRvZSJ9.K36uHGG7okpKVXOgI6V_m6nzvjFrUD47OUAtuNq_EcjNgNybTvhbtnaM6Sr2uf34b6cuLCN7fADF_CllZTp5XA --ignore-signature --ignore-expiration --verbose
```
Result :
```
INFO  | Verbose mode enabled
INFO  | Header  : {alg=HS512}
INFO  | Body    : {iat=1519492832, exp=1519492833, username=john.doe}
INFO  | Expired : true
WARN  | !!! Token signature has been ignored !!!
{iat=1519492832, exp=1519492833, username=john.doe}
```
