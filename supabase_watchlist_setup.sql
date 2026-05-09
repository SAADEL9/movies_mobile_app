-- Run this in Supabase SQL Editor.
-- It creates the table used by Watchlist and "Mark as Watched".

create table if not exists public.watchlist (
    id bigserial primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    movie_id integer not null,
    title text not null,
    poster_path text,
    rating double precision default 0,
    added_at timestamptz not null default now(),
    watched boolean not null default false,
    unique (user_id, movie_id)
);

alter table public.watchlist
    add column if not exists watched boolean not null default false;

alter table public.watchlist enable row level security;

drop policy if exists "Users can read their watchlist" on public.watchlist;
create policy "Users can read their watchlist"
on public.watchlist
for select
to authenticated
using (auth.uid() = user_id);

drop policy if exists "Users can insert their watchlist" on public.watchlist;
create policy "Users can insert their watchlist"
on public.watchlist
for insert
to authenticated
with check (auth.uid() = user_id);

drop policy if exists "Users can update their watchlist" on public.watchlist;
create policy "Users can update their watchlist"
on public.watchlist
for update
to authenticated
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

drop policy if exists "Users can delete their watchlist" on public.watchlist;
create policy "Users can delete their watchlist"
on public.watchlist
for delete
to authenticated
using (auth.uid() = user_id);
