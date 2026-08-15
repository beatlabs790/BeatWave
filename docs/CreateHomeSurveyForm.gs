// ============================================================
//  BeatWave — Home Screen & Playlist Design Survey (Google Form)
// ============================================================
//  HOW TO USE:
//  1. Go to https://script.google.com
//  2. Click "New project", delete the sample code, paste ALL of this.
//  3. Click "Run" -> "createSurveyForm". Allow permissions.
//  4. Wait a few seconds. A new Form appears in your Google Drive.
//  5. Open it, click the Send button, share the link with your users.
// ============================================================

function mc(form, title, options, helpText, showOther) {
  const item = form.addMultipleChoiceItem();
  item.setTitle(title);
  item.setChoices(options.map(function (o) { return item.createChoice(o); }));
  if (helpText) item.setHelpText(helpText);
  if (showOther) item.showOtherOption(true);
  return item;
}

function cb(form, title, options, helpText) {
  const item = form.addCheckboxItem();
  item.setTitle(title);
  item.setChoices(options.map(function (o) { return item.createChoice(o); }));
  if (helpText) item.setHelpText(helpText);
  return item;
}

function createSurveyForm() {
  const form = FormApp.create('BeatWave — Help us make your music app calmer');
  form.setDescription(
    'We are redesigning the Home screen to feel calmer and cleaner. ' +
    'No right or wrong answers — tick what feels good and leave the rest. About 10 minutes.');
  form.setCollectEmail(false);
  form.setAllowResponseEdits(true);
  form.setConfirmationMessage('Thank you! Your answers decide what we build.');

  // ----------------------------------------------------------
  // PART A — Your Home screen today
  // ----------------------------------------------------------
  form.addSectionHeaderItem()
    .setTitle('Part A — Your Home screen today')
    .setHelpText('Your Home screen currently shows many different things at once: filter chips, a tile grid that swipes like phone icons, sideways song rows, giant community cards, big cards with text on top of the artwork, wide pill buttons, a blurred photo background — and the section order shuffles randomly on every refresh. Apple Music instead uses one simple repeating card: square picture, rounded corners, title below the picture, calm solid background, fixed order.');

  mc(form, 'A1. Pick ONE sentence that matches your feeling when you open the Home screen:', [
    'It feels alive and full of things to discover.',
    'It feels a little busy / cluttered.',
    'I like the variety — different shapes keep it interesting.',
    'I get overwhelmed and scroll past most of it.',
    'I am used to it, I do not think about it.'
  ]);

  cb(form, 'A2. Which of these bother you? (tick as many as you like)', [
    'Too many different card sizes and shapes on one screen',
    'Text written on top of pictures (hard to read)',
    'The blurred photo in the background (makes everything harder to see)',
    'Sections move around to random places every refresh',
    'The "Speed Dial" grid looks like phone app icons, not music',
    'The giant "From the Community" cards are too big and busy',
    'Too many extras (promo card, floating button, chips)',
    'Nothing bothers me, it is fine'
  ]);

  form.addParagraphTextItem()
    .setTitle('A3. If we could change ONE thing to make Home feel calmer, what would it be?');

  mc(form, 'A4. Should all cards look the same (one size, one shape), like Apple?',
    ['Yes — one uniform card everywhere feels calm',
     'No — I like variety',
     'Yes, but keep small differences (circles for artists, squares for albums)'],
    'No speed or battery impact — pure design change.');

  mc(form, 'A5. Should we stop shuffling the section order and keep it fixed?', [
    'Yes — fixed order, I can find things faster',
    'No — I like surprises',
    'Keep shuffling but keep the TOP section fixed'
  ]);

  mc(form, 'A6. Should the blurred photo background stay?',
    ['Yes, I set it myself and love it',
     'Remove it by default, but keep it as an option in Settings',
     'Remove it completely — solid background only'],
    'Heads up: a blurred photo re-renders every frame the list moves, which uses CPU and can drain battery on low-end phones. Removing it is the cheapest option; keeping it is the costliest.');

  mc(form, 'A7. Do you use the "Speed Dial" grid?', [
    'Yes, every day',
    'Sometimes',
    'Never — I would be happy if it were gone',
    'I would use it more if it looked less like a phone app launcher'
  ]);

  // ----------------------------------------------------------
  // PART B — Design choices: making thumbnails consistent
  // ----------------------------------------------------------
  form.addSectionHeaderItem()
    .setTitle('Part B — Design choices: the "make thumbnails consistent" problem')
    .setHelpText('There is no correct answer — we want to know which look feels right. Imagine you are designing the screen yourself. All of these are free to change (shapes, sizes, corners, spacing).');

  mc(form, 'B1. Every picture is different, but should the FRAME be the same?',
    ['All squares — every thumbnail the same square shape (calm, uniform)',
     'All rounded squares ("squircle") — square but with softer corners (like Apple)',
     'Keep circles for artists only — one allowed exception',
     'Keep the mix — variety is part of the app personality'],
    'Right now albums/playlists are squares, artists are circles, songs are squares, and Mood & Genres are wide pills with no picture.');

  mc(form, 'B2. How round should the corners be?', [
    'Sharp (tiny corners) — like Apple Music web, modern and minimal',
    'Medium — close to what we have now',
    'Very round — almost like a capsule, soft and friendly',
    'No corners at all — plain rectangles, no shape'
  ]);

  mc(form, 'B3. Should every thumbnail be the SAME SIZE, or can sections be bigger/smaller?', [
    'One size everywhere — strongest rhythm, very Apple',
    'Two sizes — normal tiles + a big "hero" tile for special picks',
    'Keep several sizes — big cards and small tiles mixed'
  ]);

  mc(form, 'B4. How many tiles should fit in one row?', [
    '3 per row — bigger art, easier to see',
    '4 per row — classic, balanced',
    'More — denser, more to discover at a glance',
    'I do not care — you decide'
  ]);

  mc(form, 'B5. Where should the title go?', [
    'Below the artwork (Apple style — always readable)',
    'On the artwork (over a dark fade — looks cool, harder to read)',
    'Below, but keep "on artwork" only for the big hero card'
  ]);

  mc(form, 'B6. Should a tile have a visible card background?', [
    'Plain artwork only — picture floats on the screen background',
    'Artwork + faint card fill behind title — looks more like a "card"',
    'Artwork + full colored tile — each tile is a colored button'
  ]);

  mc(form, 'B7. How long can a title be before it gets cut?', [
    'One line (short and tidy)',
    'Two lines (rarely cuts, tiles get taller)'
  ]);

  mc(form, 'B8. The square vs rectangle question: some song videos are widescreen.', [
    'Always square — uniform, we crop the sides',
    'Square for everything EXCEPT videos (16:9)',
    'Keep whatever the original picture ratio is (ragged but honest)'
  ]);

  const b9 = cb(form, 'B9. If we had to keep only THREE kinds of things on Home, pick your three:', [
    'Square picture tiles (album/playlist/artist)',
    'Song rows (small thumb + title + artist, like a queue)',
    'Big hero card (the "star" of the day)',
    'Chips / filter tags',
    'Pill buttons (Mood & Genres)',
    'The Speed Dial grid',
    'The giant "From the Community" card'
  ]);
  b9.setValidation(FormApp.createCheckboxValidation().requireSelectExactly(3).build());

  mc(form, 'B10. The 30-second design test. What should the Home screen "say" to a friend opening the app?',
    ['Everything here is easy to find.',
     'This looks cool and different.',
     'I do not know where to look first.'],
    null, true);

  // ----------------------------------------------------------
  // PART C — Playlist colors (the mood problem)
  // ----------------------------------------------------------
  form.addSectionHeaderItem()
    .setTitle('Part C — Playlist colors (the mood problem)')
    .setHelpText('When you change a playlist cover picture, the whole playlist screen changes color to match that picture. Orange cover = orange screen. There is no way to keep a calm neutral look (grey, black) — the picture always decides the mood for you.');

  mc(form, 'C1. Does this problem feel real to you?', [
    'Yes! I have noticed the screen "shouts" a color I did not choose',
    'Kind of — I never really thought about it',
    'No, I like that the color follows the picture'
  ]);

  mc(form, 'C2. What do you prefer?', [
    'Neutral / dark (grey, black) — calm always',
    'Color from the cover — lively, but I cannot control it',
    'A mix — give me both choices'
  ]);

  mc(form, 'C3. Should a user be able to pick the playlist background color themselves?',
    ['Yes — let me choose per playlist (playlist A = dark grey, playlist B = black)',
     'Yes — but one choice for the whole app in Settings',
     'No — keep it automatic',
     'Not sure — I would need to see it first'],
    'Nearly free, and it actually saves battery — picking your own color skips the work we currently do to extract a color from every cover.');

  form.addParagraphTextItem()
    .setTitle('C4. If you chose "no" or "not sure" — what would convince you?');

  form.addPageBreakItem().setTitle('C5. Rate how good each idea sounds (1 = bad, 5 = great)');

  form.addScaleItem().setTitle('Auto color from cover (what we have now)')
    .setBounds(1, 5).setLabels('Bad', 'Great');
  form.addScaleItem().setTitle('Pick one color per playlist yourself')
    .setBounds(1, 5).setLabels('Bad', 'Great');
  form.addScaleItem().setTitle('Pick one app-wide color in Settings')
    .setBounds(1, 5).setLabels('Bad', 'Great');
  form.addScaleItem().setTitle('Always neutral dark, no colors at all')
    .setBounds(1, 5).setLabels('Bad', 'Great');

  mc(form, 'C6. Where should the color option live?', [
    'Inside the playlist edit menu (next to rename / change cover)',
    'In Settings',
    'Both',
    'Long-press the playlist → "change color"'
  ]);

  // ----------------------------------------------------------
  // PART D — Cheat sheet check
  // ----------------------------------------------------------
  form.addPageBreakItem().setTitle('Part D — The "is this solution worth it?" check');

  form.addParagraphTextItem()
    .setTitle('D1. Use the cheat sheet on the idea you liked most from Part A, B or C.')
    .setHelpText('Run the idea through these four checks:\n' +
      '1) Is it calm? (a good fix reduces noise)\n' +
      '2) Is it cheap to run? (colors/shapes/spacing = free; heavy blur, many big images, animation = expensive)\n' +
      '3) Does it make music more readable? (title on plain surface beats title on busy photo)\n' +
      '4) Does it respect both kinds of users? (color lovers AND neutral lovers)\n\n' +
      'Write: your idea, the four checks, and a verdict (good / bad / needs tweaking).');

  // ----------------------------------------------------------
  // PART E — About you
  // ----------------------------------------------------------
  form.addSectionHeaderItem().setTitle('Part E — About you (optional)');

  mc(form, 'I mostly use:', ['Dark mode', 'Light mode', 'Auto']);
  mc(form, 'My phone is:', ['New / high-end', 'A few years old', 'Low-end / old']);
  mc(form, 'I am:', ['A daily heavy user', 'A casual listener', 'A playlist maker']);
  form.addParagraphTextItem()
    .setTitle('Free thoughts (anything this form missed):');

  Logger.log('Form created: ' + form.getPublishedUrl());
  return form.getPublishedUrl();
}
