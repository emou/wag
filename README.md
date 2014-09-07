# Word Association Game

    Uses websocket messages/rpc calls. Only the initial html is served through an HTTP request.
    NOTE: Generally, it's my first excersise in Clojure coded in a few days and has many bugs and unsolved problems.

This is a word association game that runs in the browser.

## General flow

    NOTE: This was simply the preliminary idea/description for the game.

1. User opens the application. He's prompted for his name.

2. After "logging in" he can either:

  - Start a new game. The game must consist of 4 people - 2 teams of 2 people.
    * The game would have an ID (pass?) the user can send to other people that can join.
    * If there are less than 4 people that want to play together, the game can
      be opened to random people to fill in the spots.

  - Join a random game that's waiting for random participants.
    * User either joins a game immediately, if there is one available, or sees a message
      'Waiting for game ...' and can either cancel the wait, or continue waiting for a game to appear.

3. When 4 people have joined a game, it can start.

  - It's the first user's turn, Bob. Bob has to think of a word.
  - Above Bob's name/profile a bubble saying "Thinking ..." appears.
  - When Bob thinks of a word, he types it in a text field.
    * This is the word that the other team needs to guess (the Guess Word).
  - One of the other team's members, Alice, receives the word.
    * TODO: How did we determine Alice and not her teammate should see the word?
  - When that happens, a "Thinking ..." bubble appears above Alice's profile.
  - Alice then thinks of a word to hint her teammate about the Guess Word.
  - Her teammate, Chochko, then must take the hint and produce a guess. Now he has the "Thinking ..." bubble.
  - Chochko makes a guess.
    * If the word matches the Guess Word exactly, Alice and Chochko win.
    * If the word doesn't match the Guess Word exactly, Bob is presented with a dialog saying
      "It seems Chochko didn't guess. Confirm?". At this point Bob can either Confirm a wrong guess, or manually pronounce a correct guess.
      This will avoid spelling mistakes, other forms of the same word, etc.

Extra Features that are no the radar, but not definite:

- Store all the associations between words that users make.
- Use the stored associations to make a "bot".
- Have a "learn" interface in which you can just enter words and associations between them manually which
  will be used by the "bot". This will be useful to bootstrap the bot when there is no data from played games.
- You'd then be able to play against the "bot". It'd be nice if the both was able to make.
- Display a graph of word associations.

## Usage

```bash
% lein run -- --help
Usage:

 Switches           Default  Desc
 --------           -------  ----
 -i, --ip                    The ip address to bind to
 -p, --port                  Port to listen
 -t, --thread                Http worker thread count
 --no-help, --help  false    Print this help
```

Run with application defaults:

```bash
% lein run
```
...then point browser to [localhost:8080](http://localhost:8080)

Application defaults are supplied by environment-based configuration
found within `./resources-dev/config.clj`.

## Compiling ClojureScript

The easiest way in development mode is to run the ClojureScript compier in the
`auto` mode:

```
lein cljsbuild auto dev
```

## License

Copyright © 2014 Emil Stanchev

Distributed under the Eclipse Public License, the same as Clojure.
