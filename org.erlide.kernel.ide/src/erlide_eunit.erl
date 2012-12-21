%% Author: jakob
%% Created: 12 nov 2010
%% Description: TODO: Add description to erlide_eunit
-module(erlide_eunit).

%%
%% Include files
%%

%% -define(DEBUG, 1).

-include("erlide.hrl").

%%
%% Exported Functions
%%

-export([find_tests/1, run_tests/2, count_tests/1]).

%%
%% API Functions
%%

find_tests(Beams) ->
    R = get_exported_tests(Beams),
    {ok, R}.

count_tests(List) ->
    count_tests(List, []).

run_tests(Tests, JPid) ->
    EUnitTests = get_tests(Tests),
    eunit:test(EUnitTests, [{report, {erlide_eunit_listener, [{jpid, JPid}]}}]),
    timer:sleep(10000),
    erlang:halt().

%%
%% Local Functions
%%

-record(test, {m, f}).
-record(generated, {m, f}).

%% from eunit_internal...
-record(group, {desc = undefined,
                order = undefined,      % run in order or in parallel
                timeout = undefined,
                context = undefined,    % setup-context record
                spawn = undefined,      % run group in new process
                tests = undefined}).


count_tests([], Acc) ->
    {ok, lists:reverse(Acc)};
count_tests([{generated, M, F} | Rest], Acc) ->
    Iter = eunit_data:iter_init({generator, M, F}, "eunit_erlide"),
    N = count_generated_tests({start, Iter}, 0),
    count_tests(Rest, [{N} | Acc]);
count_tests([{test, _Fun} | Rest], Acc) ->
    count_tests(Rest, [{1} | Acc]);
count_tests([{test, _M, _F} | Rest], Acc) ->
    count_tests(Rest, [{1} | Acc]).

count_generated_tests(none, Acc) ->
    ?D(Acc),
    Acc;
count_generated_tests({start, none}, Acc) ->
    ?D(Acc),
    Acc;
count_generated_tests({#group{tests=Tests}, Iter}, Acc) ->
    GroupIter = eunit_data:iter_init(Tests, eunit_data:iter_id(Iter)),
    NewAcc = count_generated_tests({start, GroupIter}, Acc),
    count_generated_tests(eunit_data:iter_next(Iter), NewAcc);
count_generated_tests({_Test, Iter}, Acc) ->
    ?D(Iter),
    count_generated_tests(eunit_data:iter_next(Iter), Acc+1).

get_exported_tests(Beams) ->
    get_exported_tests(Beams, []).

get_exported_tests([], Acc) ->
    lists:reverse(Acc);
get_exported_tests([Beam | Rest], Acc) ->
    NewAcc = get_exported_tests_aux(Beam, Acc),
    get_exported_tests(Rest, NewAcc).

get_exported_tests_aux(Beam, Acc) ->
    {ok, Chunks} = beam_lib:chunks(Beam, [exports]),
    {Module, ExportsList} = Chunks,
    get_tests(ExportsList, Module, Beam, Acc).

get_tests([], _M, _B, Acc) ->
    Acc;
get_tests([{exports, Exports} | Rest], Module, Beam, Acc) ->
    NewAcc = get_tests_aux(Exports, Module, Beam, Acc),
    get_tests(Rest, Module, Beam, NewAcc).

get_tests_aux([], _M, _B, Acc) ->
    Acc;
get_tests_aux([{F, 0} | Rest], Module, Beam, Acc) ->
    Name = atom_to_list(F),
    case is_generator_name(Name) of
        true ->
            get_tests_aux(Rest, Module, Beam, [#generated{m=Module, f=F} | Acc]);
        false ->
            case is_test_name(Name) of
                true ->
                    get_tests_aux(Rest, Module, Beam, [#test{m=Module, f=F} | Acc]);
                false ->
                    get_tests_aux(Rest, Module, Beam, Acc)
            end
    end;
get_tests_aux([_ | Rest], Module, Beam, Acc) ->
    get_tests_aux(Rest, Module, Beam, Acc).

is_generator_name(Name) ->
    lists:suffix("_test_", Name).

is_test_name(Name) ->
    lists:suffix("_test", Name).

get_tests(Tests) ->
    get_tests(Tests, []).

get_tests([], Acc) ->
    lists:reverse(Acc);
get_tests([#generated{m=Module, f=F} | Rest], Acc) ->
    get_tests(Rest, (lists:reverse(Module:F(), Acc)));
get_tests([Test | Rest], Acc) ->
    get_tests(Rest, [Test | Acc]).
