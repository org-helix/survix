# The `.survix.json` file format

Survix stores and exports surveys as readable, versioned JSON. A stored survey (in the
app's private storage, one file per survey) and an exported one are the same file, so any
tool that reads JSON can reuse a survey, and any file following this page can be imported.

## Envelope

```json
{
  "format": "survix",
  "version": 1,
  "surveys": [ { "...": "a survey" } ]
}
```

| Field | Type | Meaning |
|---|---|---|
| `format` | string | Always `"survix"`. |
| `version` | integer | Format version. Survix refuses files with a higher version than it knows, and says the app needs updating. |
| `surveys` | array | One or more surveys. "Export all" writes every survey into one file. |

A bare survey object (with a `blocks` array and no envelope) is also accepted on import.
Unknown fields are ignored, so files can carry extra data.

## Survey

| Field | Type | Meaning |
|---|---|---|
| `id` | string | Letters, digits, `-` or `_`, at most 64 characters. Importing never overwrites: a survey whose id already exists gets a new one. |
| `title` | string | |
| `description` | string | Introduction shown before the first block. |
| `blocks` | array | The blocks, in order. |
| `createdAt`, `updatedAt` | integer | Milliseconds since 1970-01-01 UTC. |

## Blocks

Every block has an `id` and a `type`. Every block except `section` is a question and also has
`label` (the question), `help` (a note under it) and `required` (boolean).

| `type` | Extra fields |
|---|---|
| `section` | `title`, `text`. A heading; asks nothing and is not numbered. |
| `text` | `long` (boolean: paragraph instead of one line), `placeholder`. |
| `choice` | `options` (array of `{ "id", "label" }`), `multiple` (boolean: several answers), `allowOther` (boolean: an "Other" option with a free-text field). |
| `yes_no` | — |
| `rating` | `max` (2 to 10: the scale runs from 1 to `max`), `minLabel`, `maxLabel`. |
| `number` | `min`, `max` (optional numbers), `unit`. |
| `date` | — |

When reading a file, Survix repairs what it safely can: blank or duplicated ids get new ones,
a `rating.max` outside 2–10 is clamped, and number bounds given in the wrong order are swapped.

## Example

```json
{
  "format": "survix",
  "version": 1,
  "surveys": [
    {
      "id": "6c0d2b8e-3f5a-4d3b-9f43-0f0d5e1c2a77",
      "title": "Your feedback after the visit",
      "description": "Five minutes to help us welcome you better.",
      "blocks": [
        { "type": "section", "id": "b1", "title": "Your visit", "text": "" },
        { "type": "rating", "id": "b2", "label": "How would you rate the welcome?", "help": "",
          "required": true, "max": 5, "minLabel": "Poor", "maxLabel": "Excellent" },
        { "type": "choice", "id": "b3", "label": "What could be improved?", "help": "",
          "required": false, "multiple": true, "allowOther": false,
          "options": [ { "id": "o1", "label": "Waiting time" }, { "id": "o2", "label": "Signs" } ] }
      ],
      "createdAt": 1763769600000,
      "updatedAt": 1763769600000
    }
  ]
}
```

## Markdown export

"Export as text (.md)" writes the survey as Markdown, in the interface language: numbered
questions, `- [ ]` boxes for options, blank lines to write on. It is meant for printing or for
pasting into a document, and cannot be imported back.
