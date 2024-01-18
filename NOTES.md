Notes
=====

Ideas
-----

* by ID pointer: 
  - `since`, 
  - `preceding`, 
  - `from`, 
  - `to`
* by date: 
  - `observed-before`, 
  - `observed-after`, 
  - `occurred-before`, 
  - `occurred-after`
* by content: 
  - `type`, 
  - `stream`, 
  - `category`,
* sorting: 
  - `sort` (ascending/asc, descending/desc)
* filtering:
  - `filter?`
* pagination:
  - `pick`, 
  - `per-page`, 
  - `page`
* links: 
  - `first`, 
  - `last`, 
  - `next`, 
  - `previous`
  - `first` easy, no `since` / `page` / `preceding`,
    - retain `pick` / `per-page` and filters
  - `last` hard as requires count
    - does knowing last event help?
    - could find (pick + 1) events back from last and use `since`

* paging modes:
  - `since`/`preceding` + `pick`, excluding event with id, guaranteed not to 
    skip
  - `from`/`to` + `pick`, including event with id, guaranteed not to skip
  - `page` + `per-page`, may involve skips

* { 
  - `id`, 
  - `type`, 
  - `stream`, 
  - `category`, 
  - `creator`, 
  - `observed-at`, 
  - `occurred-at` 
  }

Plan
----

* only support [{since, preceding}, {pick}], not [{page}, {per-page}]
* only support [{first, next, previous}, {sort}], not [{last}, {}]
* support, type, stream, category, {observed|occurred}-{before|after}

Done
----

* [{since, preceding}, {pick}]
* [{first, next, previous}, {}]

To Do
-----

* [{}, {sort}]
* filter by [type, stream, category, {observed|occurred}-{before|after}]