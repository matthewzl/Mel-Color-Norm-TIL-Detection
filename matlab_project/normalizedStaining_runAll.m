%%% normalizedStating_runAll
clc()
files = dir('orig_ims/*.tif');
nFile = length(files);

for i = 1:nFile
    [filepath,name,ext] = fileparts(files(i).name);

    if strcmp(name, 'reference')
        I = imread(['orig_ims/' files(i).name]);
        [HE, maxC] = getReferenceData(I);
        disp("Extracted reference HE: ")
        disp(HE)
        disp("Extracted reference maxC: ")
        disp(maxC)
    end
end

for i = 1:nFile

    [filepath,name,ext] = fileparts(files(i).name);
    if strcmp(name, 'reference') == 1
        disp("(Reference file skipped...)")
        continue
    end

    display(['Working on file ' num2str(i) ': ' name])

    I = imread(['orig_ims/' files(i).name]);
    [Inorm, H, E] = normalizeStainingMod(I, HE, maxC);

    outname = strrep(files(i).name,'.tif','.tif');
    imwrite(Inorm,['norm_ims/' outname])

    %I = bfopen(['orig_ims/' files(i).name]);

    %[Inorm, H, E] = normalizeStaining(I);

    %outname = strrep(files(i).name,'.tif','.tif');
    %imwrite(Inorm,['norm_ims/' outname])

end

disp('Done')